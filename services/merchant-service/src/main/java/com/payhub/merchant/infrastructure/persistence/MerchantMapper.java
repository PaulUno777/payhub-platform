package com.payhub.merchant.infrastructure.persistence;

import com.payhub.merchant.domain.Merchant;
import com.payhub.merchant.domain.WalletRefs;

final class MerchantMapper {

    private MerchantMapper() {
    }

    static Merchant toDomain(MerchantJpaEntity entity) {
        WalletRefs wallets = null;
        if (entity.getMerchantWalletId() != null && entity.getSettlementWalletId() != null) {
            wallets = new WalletRefs(entity.getMerchantWalletId(), entity.getSettlementWalletId());
        }
        return Merchant.rehydrate(
                entity.getId(),
                entity.getLegalName(),
                entity.getStatus(),
                entity.getTier(),
                entity.getAssignedRuleSetKey(),
                entity.getFinLedgerTenantId(),
                wallets,
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static MerchantJpaEntity toEntity(Merchant merchant) {
        MerchantJpaEntity entity = new MerchantJpaEntity();
        entity.setId(merchant.id());
        entity.setLegalName(merchant.legalName());
        entity.setStatus(merchant.status());
        entity.setTier(merchant.tier());
        entity.setAssignedRuleSetKey(merchant.assignedRuleSetKey());
        entity.setFinLedgerTenantId(merchant.finLedgerTenantId());
        if (merchant.walletRefs() != null) {
            entity.setMerchantWalletId(merchant.walletRefs().merchantWalletId());
            entity.setSettlementWalletId(merchant.walletRefs().settlementWalletId());
        }
        entity.setRejectionReason(merchant.rejectionReason());
        entity.setCreatedAt(merchant.createdAt());
        entity.setUpdatedAt(merchant.updatedAt());
        return entity;
    }
}
