package com.payhub.merchant.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.payhub.merchant.domain.Merchant;
import com.payhub.merchant.domain.WalletRefs;

public record MerchantView(
        UUID id,
        String legalName,
        String status,
        String tier,
        String assignedRuleSetKey,
        UUID finLedgerTenantId,
        UUID merchantWalletId,
        UUID settlementWalletId,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static MerchantView from(Merchant merchant) {
        WalletRefs wallets = merchant.walletRefs();
        return new MerchantView(
                merchant.id(),
                merchant.legalName(),
                merchant.status().name(),
                merchant.tier().name(),
                merchant.assignedRuleSetKey(),
                merchant.finLedgerTenantId(),
                wallets == null ? null : wallets.merchantWalletId(),
                wallets == null ? null : wallets.settlementWalletId(),
                merchant.rejectionReason(),
                merchant.createdAt(),
                merchant.updatedAt()
        );
    }
}
