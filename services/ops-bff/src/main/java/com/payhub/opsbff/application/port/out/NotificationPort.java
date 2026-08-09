package com.payhub.opsbff.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationPort {

    List<WebhookDeliveryDto> listDeadLetterQueue(UUID tenantId, int limit);

    record WebhookDeliveryDto(
            UUID id,
            UUID eventId,
            UUID paymentId,
            UUID tenantId,
            String paymentStatus,
            Instant occurredAt,
            String endpointUrl,
            String status,
            int attemptCount,
            Instant nextAttemptAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
