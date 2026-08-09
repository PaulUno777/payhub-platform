package com.payhub.notification.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface WebhookTransportPort {

    DeliveryResult deliver(WebhookPayload payload, String endpointUrl);

    record WebhookPayload(
            UUID eventId,
            UUID paymentId,
            UUID tenantId,
            String status,
            Instant occurredAt
    ) {
    }

    record DeliveryResult(boolean success, int httpStatus, String errorMessage) {

        public static DeliveryResult ok(int httpStatus) {
            return new DeliveryResult(true, httpStatus, null);
        }

        public static DeliveryResult failed(int httpStatus, String errorMessage) {
            return new DeliveryResult(false, httpStatus, errorMessage);
        }
    }
}
