package com.payhub.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.payhub.notification.domain.WebhookDelivery;
import com.payhub.notification.domain.WebhookDeliveryStatus;

public record WebhookDeliveryView(
        UUID id,
        UUID eventId,
        UUID paymentId,
        UUID tenantId,
        String paymentStatus,
        Instant occurredAt,
        String endpointUrl,
        WebhookDeliveryStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {

    public static WebhookDeliveryView from(WebhookDelivery delivery) {
        return new WebhookDeliveryView(
                delivery.id(),
                delivery.eventId(),
                delivery.paymentId(),
                delivery.tenantId(),
                delivery.paymentStatus(),
                delivery.occurredAt(),
                delivery.endpointUrl(),
                delivery.status(),
                delivery.attemptCount(),
                delivery.nextAttemptAt(),
                delivery.lastError(),
                delivery.createdAt(),
                delivery.updatedAt()
        );
    }
}
