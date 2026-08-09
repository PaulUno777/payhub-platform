package com.payhub.opsbff.application.port.out;

import java.util.UUID;

/**
 * Thin outbound client to merchant-service — no domain logic in Ops BFF.
 */
public interface MerchantServicePort {

    MerchantDto get(UUID merchantId);

    MerchantDto approve(UUID merchantId, String idempotencyKey, String authorizationHeader);

    MerchantDto reject(UUID merchantId, String reason, String idempotencyKey);

    record MerchantDto(
            UUID id,
            String legalName,
            String status,
            String tier,
            String assignedRuleSetKey,
            UUID finLedgerTenantId,
            UUID merchantWalletId,
            UUID settlementWalletId,
            String rejectionReason
    ) {
    }
}
