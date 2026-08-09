package com.payhub.merchant.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.payhub.merchant.domain.MerchantStatus;
import com.payhub.merchant.domain.MerchantTier;

@Entity
@Table(name = "merchant")
public class MerchantJpaEntity {

    @Id
    private UUID id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MerchantStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MerchantTier tier;

    @Column(name = "assigned_rule_set_key", nullable = false, length = 128)
    private String assignedRuleSetKey;

    @Column(name = "fin_ledger_tenant_id")
    private UUID finLedgerTenantId;

    @Column(name = "merchant_wallet_id")
    private UUID merchantWalletId;

    @Column(name = "settlement_wallet_id")
    private UUID settlementWalletId;

    @Column(name = "rejection_reason", length = 512)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MerchantJpaEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public MerchantStatus getStatus() { return status; }
    public void setStatus(MerchantStatus status) { this.status = status; }
    public MerchantTier getTier() { return tier; }
    public void setTier(MerchantTier tier) { this.tier = tier; }
    public String getAssignedRuleSetKey() { return assignedRuleSetKey; }
    public void setAssignedRuleSetKey(String assignedRuleSetKey) { this.assignedRuleSetKey = assignedRuleSetKey; }
    public UUID getFinLedgerTenantId() { return finLedgerTenantId; }
    public void setFinLedgerTenantId(UUID finLedgerTenantId) { this.finLedgerTenantId = finLedgerTenantId; }
    public UUID getMerchantWalletId() { return merchantWalletId; }
    public void setMerchantWalletId(UUID merchantWalletId) { this.merchantWalletId = merchantWalletId; }
    public UUID getSettlementWalletId() { return settlementWalletId; }
    public void setSettlementWalletId(UUID settlementWalletId) { this.settlementWalletId = settlementWalletId; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
