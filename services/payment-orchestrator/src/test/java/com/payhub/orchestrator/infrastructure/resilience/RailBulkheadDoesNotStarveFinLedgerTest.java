package com.payhub.orchestrator.infrastructure.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.application.port.out.LedgerPort;
import com.payhub.orchestrator.application.port.out.LedgerPort.ConfirmSettlementCommand;
import com.payhub.orchestrator.application.port.out.RailPort;
import com.payhub.orchestrator.application.port.out.RailPort.RailResult;
import com.payhub.orchestrator.application.port.out.RailPort.RailSubmitCommand;
import com.payhub.orchestrator.infrastructure.ledger.FinLedgerClient;
import com.payhub.orchestrator.infrastructure.rail.HttpRailPort;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Exit criterion DS-015: a slow rail must not exhaust FinLedger capacity.
 * Separate pools + rail bulkhead=2; FinLedger calls still succeed under concurrent rail pressure.
 */
@Tag("integration")
class RailBulkheadDoesNotStarveFinLedgerTest {

    private MockWebServer railServer;
    private MockWebServer ledgerServer;
    private RailPort railPort;
    private LedgerPort ledgerPort;
    private TestDependencyResilience resilienceFixture;

    @BeforeEach
    void setUp() throws IOException {
        railServer = new MockWebServer();
        ledgerServer = new MockWebServer();
        railServer.start();
        ledgerServer.start();

        ResilienceProperties.DependencyConfig railCfg = new ResilienceProperties.DependencyConfig(
                2, 2, 2, Duration.ZERO, Duration.ofSeconds(5), Duration.ofSeconds(1),
                Duration.ofSeconds(10), 100f, 1, false
        );
        ResilienceProperties.DependencyConfig ledgerCfg = new ResilienceProperties.DependencyConfig(
                8, 8, 8, Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1),
                Duration.ofSeconds(10), 100f, 1, false
        );
        ResilienceProperties.DependencyConfig riskCfg = ResilienceProperties.DependencyConfig.defaults(
                4, 2, 2, Duration.ofSeconds(1), Duration.ofSeconds(1), false
        );
        resilienceFixture = TestDependencyResilience.create(railCfg, ledgerCfg, riskCfg);

        RestClient railClient = OutboundResilienceConfig.buildClient(
                railCfg,
                railServer.url("/").toString().replaceAll("/$", "")
        );
        RestClient ledgerClient = OutboundResilienceConfig.buildClient(
                ledgerCfg,
                ledgerServer.url("/").toString().replaceAll("/$", "")
        );

        railPort = new HttpRailPort(railClient, resilienceFixture.resilience());
        ledgerPort = new FinLedgerClient(ledgerClient, resilienceFixture.resilience());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (resilienceFixture != null) {
            resilienceFixture.close();
        }
        railServer.shutdown();
        ledgerServer.shutdown();
    }

    @Test
    void slow_rail_bulkhead_does_not_starve_finledger_pool() throws Exception {
        for (int i = 0; i < 8; i++) {
            railServer.enqueue(new MockResponse()
                    .setBody("{\"outcome\":\"ACCEPTED\",\"providerReference\":\"p-" + i + "\"}")
                    .addHeader("Content-Type", "application/json")
                    .setBodyDelay(800, TimeUnit.MILLISECONDS));
        }
        for (int i = 0; i < 8; i++) {
            ledgerServer.enqueue(new MockResponse()
                    .setBody("{\"railReference\":\"r-1\",\"status\":\"SETTLED\",\"replayed\":false}")
                    .addHeader("Content-Type", "application/json"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ledgerSuccess = new AtomicInteger();
        AtomicInteger railAmbiguousOrAccepted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            futures.add(pool.submit(() -> {
                await(start);
                var result = railPort.submit(new RailSubmitCommand(
                        UUID.randomUUID(), "10.00", "XOF", "ACCEPT"
                ));
                if (result.outcome() == RailResult.ACCEPTED || result.outcome() == RailResult.AMBIGUOUS) {
                    railAmbiguousOrAccepted.incrementAndGet();
                }
            }));
        }
        for (int i = 0; i < 6; i++) {
            futures.add(pool.submit(() -> {
                await(start);
                ledgerPort.confirmSettlement(new ConfirmSettlementCommand(
                        UUID.randomUUID(),
                        "rail-ref-1",
                        "idem-" + UUID.randomUUID(),
                        "token"
                ));
                ledgerSuccess.incrementAndGet();
            }));
        }

        start.countDown();
        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertThat(ledgerSuccess.get())
                .as("FinLedger pool/bulkhead must remain available while rail is saturated")
                .isEqualTo(6);
        assertThat(railAmbiguousOrAccepted.get()).isEqualTo(6);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("start latch timeout");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
