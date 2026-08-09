package com.payhub.orchestrator.infrastructure.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.application.port.out.LedgerPort;
import com.payhub.orchestrator.application.port.out.LedgerPort.InitiateRailPaymentCommand;
import com.payhub.orchestrator.application.port.out.LedgerPort.InitiateRailPaymentResult;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@Tag("contract")
class FinLedgerClientIdempotencyTest {

    private MockWebServer server;
    private LedgerPort ledgerPort;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        FinLedgerProperties properties = new FinLedgerProperties(
                server.url("/").toString().replaceAll("/$", ""),
                "test-token",
                "MANUAL",
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
                false
        );
        ledgerPort = new FinLedgerClient(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void should_send_same_idempotency_key_and_surface_replay_flag_on_second_call() throws Exception {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID instructionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID journalId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String key = "payhub-rail-init-demo";
        String body = """
                {
                  "instructionId": "%s",
                  "railReference": "rail-ref-1",
                  "status": "INITIATED",
                  "initiateJournalEntryId": "%s",
                  "replayed": %s
                }
                """.formatted(instructionId, journalId, "%s");

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(body.formatted(false)));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body.formatted(true)));

        InitiateRailPaymentCommand command = new InitiateRailPaymentCommand(
                tenantId,
                key,
                "MANUAL",
                "25.00",
                "USD",
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "ord-1",
                "test-token"
        );

        InitiateRailPaymentResult first = ledgerPort.initiateRailPayment(command);
        InitiateRailPaymentResult second = ledgerPort.initiateRailPayment(command);

        assertThat(first.railReference()).isEqualTo("rail-ref-1");
        assertThat(first.replayed()).isFalse();
        assertThat(second.railReference()).isEqualTo(first.railReference());
        assertThat(second.instructionId()).isEqualTo(first.instructionId());
        assertThat(second.replayed()).isTrue();

        RecordedRequest req1 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest req2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req1).isNotNull();
        assertThat(req2).isNotNull();
        assertThat(req1.getHeader("Idempotency-Key")).isEqualTo(key);
        assertThat(req2.getHeader("Idempotency-Key")).isEqualTo(key);
        assertThat(req1.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(req1.getPath()).isEqualTo("/api/v1/tenants/" + tenantId + "/rails/payments");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void should_confirm_settlement_with_idempotency_key_on_settle_path() throws Exception {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        String railReference = "rail-ref-settle-1";
        String key = "payhub-rail-settle-demo";
        String body = """
                {
                  "railReference": "%s",
                  "status": "SETTLED",
                  "replayed": %s
                }
                """.formatted(railReference, "%s");

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body.formatted(false)));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body.formatted(true)));

        var command = new LedgerPort.ConfirmSettlementCommand(tenantId, railReference, key, "test-token");
        var first = ledgerPort.confirmSettlement(command);
        var second = ledgerPort.confirmSettlement(command);

        assertThat(first.status()).isEqualTo("SETTLED");
        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.railReference()).isEqualTo(railReference);

        RecordedRequest req1 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest req2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req1).isNotNull();
        assertThat(req2).isNotNull();
        assertThat(req1.getHeader("Idempotency-Key")).isEqualTo(key);
        assertThat(req1.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(req1.getPath()).isEqualTo(
                "/api/v1/tenants/" + tenantId + "/rails/payments/" + railReference + "/settle"
        );
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void should_post_refund_with_idempotency_key_without_fee_arithmetic() throws Exception {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID journalId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID refundId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        String key = "payhub-refund-demo";
        String body = """
                {
                  "refundId": "%s",
                  "status": "SETTLED",
                  "replayed": %s
                }
                """.formatted(refundId, "%s");

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(body.formatted(false)));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body.formatted(true)));

        var command = new LedgerPort.RefundCommand(
                tenantId,
                key,
                "rail-ref-1",
                journalId,
                "4.00",
                "USD",
                "test-token"
        );
        var first = ledgerPort.refund(command);
        var second = ledgerPort.refund(command);

        assertThat(first.refundId()).isEqualTo(refundId);
        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();

        RecordedRequest req1 = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req1).isNotNull();
        assertThat(req1.getHeader("Idempotency-Key")).isEqualTo(key);
        assertThat(req1.getPath()).isEqualTo("/api/v1/tenants/" + tenantId + "/refunds");
        String reqBody = req1.getBody().readUtf8();
        assertThat(reqBody).contains("\"refundAmount\":\"4.00\"");
        assertThat(reqBody).doesNotContain("feeReversal");
        RecordedRequest req2 = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req2).isNotNull();
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void should_put_fee_config_no_reverse() throws Exception {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        server.enqueue(new MockResponse().setResponseCode(204));

        ledgerPort.putFeeConfig(new LedgerPort.PutFeeConfigCommand(tenantId, "NO_REVERSE", "test-token"));

        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req).isNotNull();
        assertThat(req.getMethod()).isEqualTo("PUT");
        assertThat(req.getPath()).isEqualTo("/api/v1/tenants/" + tenantId + "/fee-config");
        assertThat(req.getBody().readUtf8()).contains("NO_REVERSE");
    }
}