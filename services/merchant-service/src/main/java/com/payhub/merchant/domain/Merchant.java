package com.payhub.merchant.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Merchant {

    private final UUID id;
    private final String legalName;
    private MerchantStatus status;
    private final MerchantTier tier;
    private final String assignedRuleSetKey;
    private UUID finLedgerTenantId;
    private WalletRefs walletRefs;
    private String rejectionReason;
    private final Instant createdAt;
    private Instant updatedAt;

    private Merchant(
            UUID id,
            String legalName,
            MerchantStatus status,
            MerchantTier tier,
            String assignedRuleSetKey,
            UUID finLedgerTenantId,
            WalletRefs walletRefs,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.legalName = Objects.requireNonNull(legalName);
        this.status = Objects.requireNonNull(status);
        this.tier = Objects.requireNonNull(tier);
        this.assignedRuleSetKey = Objects.requireNonNull(assignedRuleSetKey);
        this.finLedgerTenantId = finLedgerTenantId;
        this.walletRefs = walletRefs;
        this.rejectionReason = rejectionReason;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Merchant register(String legalName, MerchantTier tier, String assignedRuleSetKey) {
        Instant now = Instant.now();
        return new Merchant(
                UUID.randomUUID(),
                legalName,
                MerchantStatus.PENDING_REVIEW,
                tier,
                assignedRuleSetKey,
                null,
                null,
                null,
                now,
                now
        );
    }

    public static Merchant rehydrate(
            UUID id,
            String legalName,
            MerchantStatus status,
            MerchantTier tier,
            String assignedRuleSetKey,
            UUID finLedgerTenantId,
            WalletRefs walletRefs,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Merchant(
                id, legalName, status, tier, assignedRuleSetKey,
                finLedgerTenantId, walletRefs, rejectionReason, createdAt, updatedAt);
    }

    public void activate(UUID finLedgerTenantId, WalletRefs walletRefs) {
        if (status == MerchantStatus.ACTIVE
                && Objects.equals(this.finLedgerTenantId, finLedgerTenantId)
                && Objects.equals(this.walletRefs, walletRefs)) {
            return;
        }
        if (status != MerchantStatus.PENDING_REVIEW) {
            throw new IllegalMerchantStateException(
                    "Cannot activate merchant from status " + status);
        }
        this.finLedgerTenantId = Objects.requireNonNull(finLedgerTenantId);
        this.walletRefs = Objects.requireNonNull(walletRefs);
        this.status = MerchantStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        if (status != MerchantStatus.PENDING_REVIEW) {
            throw new IllegalMerchantStateException(
                    "Cannot reject merchant from status " + status);
        }
        this.status = MerchantStatus.REJECTED;
        this.rejectionReason = Objects.requireNonNull(reason);
        this.updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public String legalName() { return legalName; }
    public MerchantStatus status() { return status; }
    public MerchantTier tier() { return tier; }
    public String assignedRuleSetKey() { return assignedRuleSetKey; }
    public UUID finLedgerTenantId() { return finLedgerTenantId; }
    public WalletRefs walletRefs() { return walletRefs; }
    public String rejectionReason() { return rejectionReason; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
