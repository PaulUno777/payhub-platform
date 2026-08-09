package com.payhub.opsbff.application.port.out;

import java.util.UUID;

public interface OrchestratorPort {

    PaymentDto requestRefund(UUID paymentId, String amount, String idempotencyKey, String sandboxMode);

    record PaymentDto(
            UUID id,
            UUID merchantId,
            UUID tenantId,
            String amount,
            String currency,
            String clientReference,
            String status
    ) {
    }
}
