package com.payhub.orchestrator.domain;

import java.math.BigDecimal;
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
    private String railReference;
    private UUID initiateJournalEntryId;
    private BigDecimal refundedAmount;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(
            UUID id,
            UUID merchantId,
            UUID tenantId,
            Money money,
            String clientReference,
            PaymentStatus status,
            String railReference,
            UUID initiateJournalEntryId,
            BigDecimal refundedAmount,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.money = Objects.requireNonNull(money);
        this.clientReference = Objects.requireNonNull(clientReference);
        this.status = Objects.requireNonNull(status);
        this.railReference = railReference;
        this.initiateJournalEntryId = initiateJournalEntryId;
        this.refundedAmount = refundedAmount == null ? BigDecimal.ZERO : refundedAmount;
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
                null,
                null,
                BigDecimal.ZERO,
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
            String railReference,
            UUID initiateJournalEntryId,
            BigDecimal refundedAmount,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Payment(
                id,
                merchantId,
                tenantId,
                money,
                clientReference,
                status,
                railReference,
                initiateJournalEntryId,
                refundedAmount,
                createdAt,
                updatedAt
        );
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

    public void markSettlementPending(String railReference, UUID initiateJournalEntryId) {
        requireStatus(PaymentStatus.RAIL_SUBMITTED, "markSettlementPending");
        this.railReference = Objects.requireNonNull(railReference, "railReference");
        this.initiateJournalEntryId = Objects.requireNonNull(initiateJournalEntryId, "initiateJournalEntryId");
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

    public void requestRefund(BigDecimal refundAmount) {
        Objects.requireNonNull(refundAmount, "refundAmount");
        if (refundAmount.signum() <= 0) {
            throw new IllegalArgumentException("refundAmount must be positive");
        }
        if (status != PaymentStatus.SETTLED && status != PaymentStatus.REFUND_SETTLED) {
            throw new IllegalPaymentStateException("Cannot request refund from status " + status);
        }
        BigDecimal remaining = money.amount().subtract(refundedAmount);
        if (refundAmount.compareTo(remaining) > 0) {
            throw new IllegalPaymentStateException(
                    "Refund amount " + refundAmount + " exceeds remaining " + remaining);
        }
        this.status = PaymentStatus.REFUND_REQUESTED;
        touch();
    }

    public void markRefundRailSubmitted() {
        requireStatus(PaymentStatus.REFUND_REQUESTED, "markRefundRailSubmitted");
        this.status = PaymentStatus.REFUND_RAIL_SUBMITTED;
        touch();
    }

    public void markRefundFailedFinal() {
        if (status == PaymentStatus.REFUND_FAILED_FINAL) {
            return;
        }
        if (status != PaymentStatus.REFUND_RAIL_SUBMITTED) {
            throw new IllegalPaymentStateException("Cannot mark REFUND_FAILED_FINAL from status " + status);
        }
        this.status = PaymentStatus.REFUND_FAILED_FINAL;
        touch();
    }

    public void markRefundReconciliationRequired() {
        if (status == PaymentStatus.REFUND_RECONCILIATION_REQUIRED) {
            return;
        }
        if (status != PaymentStatus.REFUND_RAIL_SUBMITTED
                && status != PaymentStatus.REFUND_SETTLEMENT_PENDING) {
            throw new IllegalPaymentStateException(
                    "Cannot mark REFUND_RECONCILIATION_REQUIRED from status " + status);
        }
        this.status = PaymentStatus.REFUND_RECONCILIATION_REQUIRED;
        touch();
    }

    public void markRefundSettlementPending() {
        requireStatus(PaymentStatus.REFUND_RAIL_SUBMITTED, "markRefundSettlementPending");
        this.status = PaymentStatus.REFUND_SETTLEMENT_PENDING;
        touch();
    }

    public void markRefundSettled(BigDecimal refundAmount) {
        Objects.requireNonNull(refundAmount, "refundAmount");
        requireStatus(PaymentStatus.REFUND_SETTLEMENT_PENDING, "markRefundSettled");
        this.refundedAmount = this.refundedAmount.add(refundAmount);
        this.status = PaymentStatus.REFUND_SETTLED;
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
    public String railReference() { return railReference; }
    public UUID initiateJournalEntryId() { return initiateJournalEntryId; }
    public BigDecimal refundedAmount() { return refundedAmount; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
