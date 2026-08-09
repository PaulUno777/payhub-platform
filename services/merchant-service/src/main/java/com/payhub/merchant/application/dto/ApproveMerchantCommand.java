package com.payhub.merchant.application.dto;

import java.util.UUID;

public record ApproveMerchantCommand(
        UUID merchantId,
        String idempotencyKey,
        String bearerToken
) {
}
