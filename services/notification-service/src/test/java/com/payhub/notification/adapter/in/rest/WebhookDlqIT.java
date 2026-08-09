package com.payhub.notification.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import com.payhub.notification.TestcontainersConfiguration;
import com.payhub.notification.application.dto.PaymentLifecycleEnvelope;
import com.payhub.notification.application.dto.PaymentLifecycleEnvelope.PaymentStatusChangedPayload;
import com.payhub.notification.application.dto.WebhookDeliveryView;
import com.payhub.notification.application.port.in.DispatchPendingWebhooksUseCase;
import com.payhub.notification.application.port.in.ScheduleWebhookOnLifecycleEventUseCase;
import com.payhub.notification.domain.WebhookDeliveryStatus;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class WebhookDlqIT {

    private static final MockWebServer MERCHANT_SERVER = new MockWebServer();

    static {
        try {
            MERCHANT_SERVER.start();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ScheduleWebhookOnLifecycleEventUseCase scheduleUseCase;

    @Autowired
    private DispatchPendingWebhooksUseCase dispatchUseCase;

    private RestClient client;

    @DynamicPropertySource
    static void webhookProps(DynamicPropertyRegistry registry) {
        registry.add("payhub.kafka.enabled", () -> "false");
        registry.add("payhub.notification.webhooks.dispatch-enabled", () -> "false");
        registry.add("payhub.notification.webhooks.default-url", () -> MERCHANT_SERVER.url("/hooks").toString());
        registry.add("payhub.notification.webhooks.hmac-secret", () -> "it-hmac-secret");
        registry.add("payhub.notification.webhooks.max-attempts", () -> "2");
        registry.add("payhub.notification.webhooks.base-backoff", () -> "PT0S");
    }

    @AfterAll
    static void shutdownMerchant() throws IOException {
        MERCHANT_SERVER.shutdown();
    }

    @BeforeEach
    void setUp() {
        client = RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void failed_delivery_ends_in_observable_dlq() throws Exception {
        MERCHANT_SERVER.enqueue(new MockResponse().setResponseCode(500));
        MERCHANT_SERVER.enqueue(new MockResponse().setResponseCode(500));

        UUID tenantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-09T12:00:00Z");

        assertThat(scheduleUseCase.execute(new PaymentLifecycleEnvelope(
                eventId,
                "PaymentStatusChanged",
                "1",
                now,
                "payment-orchestrator",
                paymentId,
                tenantId,
                null,
                null,
                new PaymentStatusChangedPayload(paymentId, UUID.randomUUID(), tenantId, "SETTLED")
        ))).isTrue();

        assertThat(dispatchUseCase.execute()).isEqualTo(1);
        assertThat(dispatchUseCase.execute()).isEqualTo(1);

        List<WebhookDeliveryView> dlq = client.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/webhooks/dlq").queryParam("limit", 50).build())
                .header(WebhookDeliveryController.TENANT_HEADER, tenantId.toString())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(dlq).isNotNull().hasSize(1);
        assertThat(dlq.getFirst().status()).isEqualTo(WebhookDeliveryStatus.DEAD);
        assertThat(dlq.getFirst().paymentId()).isEqualTo(paymentId);
        assertThat(dlq.getFirst().attemptCount()).isEqualTo(2);
        assertThat(dlq.getFirst().lastError()).isNotBlank();

        RecordedRequest first = MERCHANT_SERVER.takeRequest(2, TimeUnit.SECONDS);
        assertThat(first).isNotNull();
        assertThat(first.getHeader("X-PayHub-Signature")).startsWith("sha256=");
        assertThat(first.getBody().readUtf8()).contains(paymentId.toString());
    }
}
