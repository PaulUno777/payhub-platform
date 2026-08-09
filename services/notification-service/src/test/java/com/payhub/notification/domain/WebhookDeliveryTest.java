package com.payhub.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class WebhookDeliveryTest {

    @Test
    void should_start_pending_when_endpoint_configured() {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        WebhookDelivery delivery = WebhookDelivery.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SETTLED",
                now,
                "https://merchant.example/hooks",
                now
        );

        assertThat(delivery.status()).isEqualTo(WebhookDeliveryStatus.PENDING);
        assertThat(delivery.attemptCount()).isZero();
        assertThat(delivery.nextAttemptAt()).isEqualTo(now);
    }

    @Test
    void should_start_dead_when_endpoint_missing() {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        WebhookDelivery delivery = WebhookDelivery.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SETTLED",
                now,
                "  ",
                now
        );

        assertThat(delivery.status()).isEqualTo(WebhookDeliveryStatus.DEAD);
        assertThat(delivery.lastError()).contains("not configured");
    }

    @Test
    void should_become_dead_after_max_attempts() {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        WebhookDelivery delivery = WebhookDelivery.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SETTLED",
                now,
                "https://merchant.example/hooks",
                now
        );

        delivery.claim(now);
        delivery.recordFailure("HTTP 500", 2, Duration.ofSeconds(1), now.plusSeconds(1));
        assertThat(delivery.status()).isEqualTo(WebhookDeliveryStatus.FAILED_RETRYABLE);
        assertThat(delivery.attemptCount()).isEqualTo(1);
        assertThat(delivery.nextAttemptAt()).isEqualTo(now.plusSeconds(2));

        Instant retryAt = delivery.nextAttemptAt();
        delivery.claim(retryAt);
        delivery.recordFailure("HTTP 500", 2, Duration.ofSeconds(1), retryAt.plusSeconds(1));
        assertThat(delivery.status()).isEqualTo(WebhookDeliveryStatus.DEAD);
        assertThat(delivery.attemptCount()).isEqualTo(2);
    }

    @Test
    void should_mark_delivered_from_in_flight() {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        WebhookDelivery delivery = WebhookDelivery.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SETTLED",
                now,
                "https://merchant.example/hooks",
                now
        );
        delivery.claim(now);
        delivery.markDelivered(now.plusSeconds(1));
        assertThat(delivery.status()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
        assertThat(delivery.attemptCount()).isEqualTo(1);
    }

    @Test
    void should_reject_claim_when_not_retryable() {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        WebhookDelivery delivery = WebhookDelivery.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SETTLED",
                now,
                "",
                now
        );
        assertThatThrownBy(() -> delivery.claim(now))
                .isInstanceOf(IllegalStateException.class);
    }
}
