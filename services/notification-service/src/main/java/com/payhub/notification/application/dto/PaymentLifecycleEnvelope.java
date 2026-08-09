package com.payhub.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PaymentLifecycleEnvelope(
        UUID eventId,
        String eventType,
        String schemaVersion,
        Instant occurredAt,
        String producer,
        UUID aggregateId,
        UUID tenantId,
        String traceparent,
        String causationId,
        PaymentStatusChangedPayload payload
) {

    public record PaymentStatusChangedPayload(
            UUID paymentId,
            UUID merchantId,
            UUID tenantId,
            String status
    ) {
    }
}
