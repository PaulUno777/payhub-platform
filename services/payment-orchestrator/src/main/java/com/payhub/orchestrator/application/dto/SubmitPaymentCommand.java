package com.payhub.orchestrator.application.dto;

import java.util.UUID;

public record SubmitPaymentCommand(
        UUID merchantId,
        UUID tenantId,
        String amount,
        String currencyCode,
        String clientReference,
        String idempotencyKey
) {
}
