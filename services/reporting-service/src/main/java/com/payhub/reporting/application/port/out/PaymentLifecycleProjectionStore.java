package com.payhub.reporting.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentLifecycleProjectionStore {

    void upsert(PaymentLifecycleProjection projection);

    Optional<PaymentLifecycleProjection> findByPaymentId(UUID paymentId);

    long count();

    record PaymentLifecycleProjection(
            UUID paymentId,
            UUID tenantId,
            UUID merchantId,
            UUID eventId,
            String status,
            Instant occurredAt,
            Instant asOf
    ) {
    }
}
