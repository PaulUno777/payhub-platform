package com.payhub.merchant.application.dto;

import java.util.UUID;

public record RejectMerchantCommand(
        UUID merchantId,
        String reason,
        String idempotencyKey
) {
}
