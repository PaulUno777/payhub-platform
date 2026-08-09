package com.payhub.orchestrator.infrastructure.chaos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import com.payhub.orchestrator.application.port.out.LedgerPort;
import com.payhub.orchestrator.application.port.out.LedgerPort.InitiateRailPaymentCommand;
import com.payhub.orchestrator.application.port.out.LedgerPort.InitiateRailPaymentResult;
import com.payhub.orchestrator.infrastructure.ledger.FinLedgerClient;
import com.payhub.orchestrator.infrastructure.ledger.FinLedgerProperties;
import com.payhub.orchestrator.infrastructure.resilience.OutboundResilienceConfig;
import com.payhub.orchestrator.infrastructure.resilience.ResilienceProperties;
import com.payhub.orchestrator.infrastructure.resilience.TestDependencyResilience;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * DS-017 exit: after a codified Toxiproxy cut on the FinLedger path, retries with the same
 * Idempotency-Key produce at most one financial create (no duplicate money effect).
 */
@Tag("chaos")
@Tag("integration")
@Testcontainers
class NoDuplicateFinLedgerEffectUnderToxiproxyIT {

    private static final int PROXY_PORT = 8666;
    private static final String IDEMPOTENCY_KEY = "payhub-chaos-rail-init";

    @Container
    static ToxiproxyContainer toxiproxy = new ToxiproxyContainer(
            DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.5.0")
    );

    private MockWebServer finLedger;
    private Proxy proxy;
    private String proxiedBaseUrl;
    private ResilienceProperties.DependencyConfig finledgerCfg;
    private TestDependencyResilience resilienceFixture;
    private LedgerPort ledgerPort;

    @BeforeEach
    void setUp() throws Exception {
        finLedger = new MockWebServer();
        finLedger.start();

        ToxiproxyClient client = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        // Docker Desktop: reach MockWebServer on the host via host.docker.internal
        proxy = client.createProxy(
                "finledger",
                "0.0.0.0:" + PROXY_PORT,
                "host.docker.internal:" + finLedger.getPort()
        );
        proxiedBaseUrl = "http://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(PROXY_PORT);

        finledgerCfg = new ResilienceProperties.DependencyConfig(
                8, 4, 4, Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(400),
                Duration.ofSeconds(2), 100f, 2, true
        );
        ResilienceProperties.DependencyConfig railCfg = ResilienceProperties.DependencyConfig.defaults(
                4, 2, 2, Duration.ofSeconds(3), Duration.ofSeconds(2), false
        );
        ResilienceProperties.DependencyConfig riskCfg = ResilienceProperties.DependencyConfig.defaults(
                4, 2, 2, Duration.ofSeconds(1), Duration.ofMillis(800), false
        );
        resilienceFixture = TestDependencyResilience.create(railCfg, finledgerCfg, riskCfg);
        ledgerPort = newLedgerPort();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (resilienceFixture != null) {
            resilienceFixture.close();
        }
        if (finLedger != null) {
            finLedger.shutdown();
        }
    }

    @Test
    void should_not_create_duplicate_finledger_effect_after_toxiproxy_cut_and_restore() throws Exception {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID instructionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID journalId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String bodyTemplate = """
                {
                  "instructionId": "%s",
                  "railReference": "rail-ref-chaos-1",
                  "status": "INITIATED",
                  "initiateJournalEntryId": "%s",
                  "replayed": %s
                }
                """.formatted(instructionId, journalId, "%s");

        InitiateRailPaymentCommand command = new InitiateRailPaymentCommand(
                tenantId,
                IDEMPOTENCY_KEY,
                "MANUAL",
                "25.00",
                "USD",
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "ord-chaos-1",
                "test-token"
        );

        proxy.disable();
        assertThatThrownBy(() -> ledgerPort.initiateRailPayment(command))
                .as("initiate must fail while FinLedger path is cut")
                .isInstanceOf(ResourceAccessException.class);

        proxy.enable();
        // Drop poisoned pooled connections from the cut window.
        ledgerPort = newLedgerPort();

        finLedger.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(bodyTemplate.formatted(false)));
        finLedger.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(bodyTemplate.formatted(true)));

        InitiateRailPaymentResult created = ledgerPort.initiateRailPayment(command);
        InitiateRailPaymentResult replayed = ledgerPort.initiateRailPayment(command);

        assertThat(created.replayed()).isFalse();
        assertThat(created.instructionId()).isEqualTo(instructionId);
        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.instructionId()).isEqualTo(created.instructionId());
        assertThat(replayed.railReference()).isEqualTo(created.railReference());

        List<RecordedRequest> requests = drainRequests();
        assertThat(requests).hasSize(2);
        assertThat(requests).allSatisfy(req ->
                assertThat(req.getHeader("Idempotency-Key")).isEqualTo(IDEMPOTENCY_KEY));
        assertThat(finLedger.getRequestCount()).isEqualTo(2);
    }

    private LedgerPort newLedgerPort() {
        FinLedgerProperties properties = new FinLedgerProperties(
                proxiedBaseUrl,
                "test-token",
                "MANUAL",
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
                false
        );
        RestClient restClient = OutboundResilienceConfig.buildClient(finledgerCfg, properties.baseUrl());
        return new FinLedgerClient(restClient, resilienceFixture.resilience());
    }

    private List<RecordedRequest> drainRequests() throws InterruptedException {
        List<RecordedRequest> requests = new ArrayList<>();
        for (int i = 0; i < finLedger.getRequestCount(); i++) {
            RecordedRequest req = finLedger.takeRequest(2, TimeUnit.SECONDS);
            assertThat(req).isNotNull();
            requests.add(req);
        }
        return requests;
    }
}
