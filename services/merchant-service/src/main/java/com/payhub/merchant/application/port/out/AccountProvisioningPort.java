package com.payhub.merchant.application.port.out;

import java.util.UUID;

/**
 * Merchant-service FinLedger ACL: SUB_MERCHANT tenant + wallets only.
 * Distinct from Orchestrator {@code LedgerPort} (rails/payments).
 */
public interface AccountProvisioningPort {

    ProvisionedAccounts provisionSubMerchant(ProvisionCommand command);

    record ProvisionCommand(
            String legalName,
            String idempotencyKey,
            String bearerToken
    ) {
    }

    record ProvisionedAccounts(
            UUID finLedgerTenantId,
            UUID merchantWalletId,
            UUID settlementWalletId
    ) {
    }
}
