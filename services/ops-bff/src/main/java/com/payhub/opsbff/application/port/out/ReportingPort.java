package com.payhub.opsbff.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface ReportingPort {

    PaymentLifecycleViewDto getPaymentLifecycleView(UUID tenantId, UUID paymentId);

    record PaymentLifecycleViewDto(
            UUID paymentId,
            UUID tenantId,
            UUID merchantId,
            String status,
            Instant occurredAt,
            Instant asOf,
            long stalenessMs,
            String freshness
    ) {
    }
}
