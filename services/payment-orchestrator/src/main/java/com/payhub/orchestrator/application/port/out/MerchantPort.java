package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

/**
 * Resolves merchantId → assignedRuleSetKey (stub in DS-006).
 */
public interface MerchantPort {

    MerchantSnapshot findById(UUID merchantId);

    record MerchantSnapshot(UUID merchantId, String assignedRuleSetKey, UUID finLedgerTenantId) {
    }
}
