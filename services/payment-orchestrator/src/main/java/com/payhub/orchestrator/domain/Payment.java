package com.payhub.orchestrator.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final UUID merchantId;
    private final UUID tenantId;
    private final Money money;
    private final String clientReference;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(
            UUID id,
            UUID merchantId,
            UUID tenantId,
            Money money,
            String clientReference,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.money = Objects.requireNonNull(money);
        this.clientReference = Objects.requireNonNull(clientReference);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Payment create(UUID merchantId, UUID tenantId, Money money, String clientReference) {
        Instant now = Instant.now();
        return new Payment(
                UUID.randomUUID(),
                merchantId,
                tenantId,
                money,
                clientReference,
                PaymentStatus.CREATED,
                now,
                now
        );
    }

    public static Payment rehydrate(
            UUID id,
            UUID merchantId,
            UUID tenantId,
            Money money,
            String clientReference,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Payment(id, merchantId, tenantId, money, clientReference, status, createdAt, updatedAt);
    }

    public void markRiskPending() {
        requireStatus(PaymentStatus.CREATED, "markRiskPending");
        this.status = PaymentStatus.RISK_PENDING;
        touch();
    }

    public void approveRisk() {
        if (status == PaymentStatus.RISK_APPROVED) {
            return;
        }
        if (status != PaymentStatus.CREATED && status != PaymentStatus.RISK_PENDING && status != PaymentStatus.RISK_REVIEW) {
            throw new IllegalPaymentStateException("Cannot approve risk from status " + status);
        }
        this.status = PaymentStatus.RISK_APPROVED;
        touch();
    }

    public void rejectRisk() {
        if (status == PaymentStatus.RISK_REJECTED) {
            return;
        }
        if (status != PaymentStatus.CREATED && status != PaymentStatus.RISK_PENDING && status != PaymentStatus.RISK_REVIEW) {
            throw new IllegalPaymentStateException("Cannot reject risk from status " + status);
        }
        this.status = PaymentStatus.RISK_REJECTED;
        touch();
    }

    public void markRiskReview() {
        if (status != PaymentStatus.CREATED && status != PaymentStatus.RISK_PENDING) {
            throw new IllegalPaymentStateException("Cannot mark risk review from status " + status);
        }
        this.status = PaymentStatus.RISK_REVIEW;
        touch();
    }

    public void markRailSubmitted() {
        requireStatus(PaymentStatus.RISK_APPROVED, "markRailSubmitted");
        this.status = PaymentStatus.RAIL_SUBMITTED;
        touch();
    }

    public void markFailedFinal() {
        if (status == PaymentStatus.FAILED_FINAL) {
            return;
        }
        if (status != PaymentStatus.RAIL_SUBMITTED) {
            throw new IllegalPaymentStateException("Cannot mark FAILED_FINAL from status " + status);
        }
        this.status = PaymentStatus.FAILED_FINAL;
        touch();
    }

    public void markReconciliationRequired() {
        if (status == PaymentStatus.RECONCILIATION_REQUIRED) {
            return;
        }
        if (status != PaymentStatus.RAIL_SUBMITTED && status != PaymentStatus.SETTLEMENT_PENDING) {
            throw new IllegalPaymentStateException(
                    "Cannot mark RECONCILIATION_REQUIRED from status " + status);
        }
        this.status = PaymentStatus.RECONCILIATION_REQUIRED;
        touch();
    }

    public void markSettlementPending() {
        requireStatus(PaymentStatus.RAIL_SUBMITTED, "markSettlementPending");
        this.status = PaymentStatus.SETTLEMENT_PENDING;
        touch();
    }

    public void markSettled() {
        if (status == PaymentStatus.SETTLED) {
            return;
        }
        requireStatus(PaymentStatus.SETTLEMENT_PENDING, "markSettled");
        this.status = PaymentStatus.SETTLED;
        touch();
    }

    private void requireStatus(PaymentStatus expected, String action) {
        if (status != expected) {
            throw new IllegalPaymentStateException("Cannot " + action + " from status " + status);
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID merchantId() { return merchantId; }
    public UUID tenantId() { return tenantId; }
    public Money money() { return money; }
    public String clientReference() { return clientReference; }
    public PaymentStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
