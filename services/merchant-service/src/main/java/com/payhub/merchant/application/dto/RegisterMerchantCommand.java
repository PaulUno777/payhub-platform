package com.payhub.merchant.application.dto;

public record RegisterMerchantCommand(
        String legalName,
        String tier,
        String assignedRuleSetKey,
        String idempotencyKey
) {
}
