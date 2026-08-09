package com.payhub.reporting.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope;
import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope.PaymentStatusChangedPayload;
import com.payhub.reporting.application.dto.PaymentLifecycleView;
import com.payhub.reporting.application.dto.PaymentLifecycleView.Freshness;
import com.payhub.reporting.application.port.in.ApplyPaymentLifecycleProjectionUseCase;
import com.payhub.reporting.application.port.in.GetPaymentLifecycleViewUseCase;
import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore.PaymentLifecycleProjection;
import com.payhub.reporting.application.port.out.ProjectionCachePort;
import com.redis.testcontainers.RedisContainer;

@Tag("integration")
@SpringBootTest
@Testcontainers
class PaymentLifecycleCacheAsideIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redis.getRedisPort()));
        registry.add("payhub.kafka.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.cloud.config.import-check.enabled", () -> "false");
    }

    @Autowired
    private ApplyPaymentLifecycleProjectionUseCase apply;
    @Autowired
    private GetPaymentLifecycleViewUseCase get;
    @Autowired
    private ProjectionCachePort cache;

    @Test
    void should_cache_aside_and_invalidate_on_apply() {
        UUID paymentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        assertThat(apply.execute(envelope(paymentId, tenantId, merchantId, "RISK_APPROVED", UUID.randomUUID()))).isTrue();

        PaymentLifecycleView first = get.execute(tenantId, paymentId).orElseThrow();
        assertThat(first.freshness()).isEqualTo(Freshness.NON_AUTHORITATIVE);
        assertThat(first.asOf()).isNotNull();
        assertThat(first.stalenessMs()).isGreaterThanOrEqualTo(0);
        assertThat(cache.getPayment(tenantId, paymentId)).isPresent();

        PaymentLifecycleProjection cached = cache.getPayment(tenantId, paymentId).orElseThrow();
        assertThat(cached.status()).isEqualTo("RISK_APPROVED");

        assertThat(apply.execute(envelope(paymentId, tenantId, merchantId, "SETTLED", UUID.randomUUID()))).isTrue();
        assertThat(cache.getPayment(tenantId, paymentId)).isEmpty();

        PaymentLifecycleView after = get.execute(tenantId, paymentId).orElseThrow();
        assertThat(after.status()).isEqualTo("SETTLED");
        assertThat(after.freshness()).isEqualTo(Freshness.NON_AUTHORITATIVE);
        assertThat(cache.getPayment(tenantId, paymentId)).isPresent();
    }

    private static PaymentLifecycleEnvelope envelope(
            UUID paymentId,
            UUID tenantId,
            UUID merchantId,
            String status,
            UUID eventId
    ) {
        return new PaymentLifecycleEnvelope(
                eventId,
                "PaymentStatusChanged",
                "1",
                Instant.parse("2026-08-09T12:00:00Z"),
                "payment-orchestrator",
                paymentId,
                tenantId,
                null,
                null,
                new PaymentStatusChangedPayload(paymentId, merchantId, tenantId, status)
        );
    }
}
