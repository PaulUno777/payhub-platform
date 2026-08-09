package com.payhub.orchestrator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PaymentTest {

    @Test
    void should_create_in_created_status() {
        Payment payment = sample();
        assertThat(payment.status()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void should_transition_created_to_risk_approved() {
        Payment payment = sample();
        payment.markRiskPending();
        payment.approveRisk();
        assertThat(payment.status()).isEqualTo(PaymentStatus.RISK_APPROVED);
    }

    @Test
    void should_reject_from_created() {
        Payment payment = sample();
        payment.rejectRisk();
        assertThat(payment.status()).isEqualTo(PaymentStatus.RISK_REJECTED);
    }

    @Test
    void should_mark_review_from_pending() {
        Payment payment = sample();
        payment.markRiskPending();
        payment.markRiskReview();
        assertThat(payment.status()).isEqualTo(PaymentStatus.RISK_REVIEW);
    }

    @Test
    void should_forbid_approve_from_rejected() {
        Payment payment = sample();
        payment.rejectRisk();
        assertThatThrownBy(payment::approveRisk).isInstanceOf(IllegalPaymentStateException.class);
    }

    @Test
    void should_reach_failed_final_on_psp_reject_path() {
        Payment payment = riskApproved();
        payment.markRailSubmitted();
        payment.markFailedFinal();
        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED_FINAL);
    }

    @Test
    void should_reach_reconciliation_required_before_pending() {
        Payment payment = riskApproved();
        payment.markRailSubmitted();
        payment.markReconciliationRequired();
        assertThat(payment.status()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);
    }

    @Test
    void should_reach_settled_on_happy_path() {
        Payment payment = riskApproved();
        payment.markRailSubmitted();
        payment.markSettlementPending();
        payment.markSettled();
        assertThat(payment.status()).isEqualTo(PaymentStatus.SETTLED);
    }

    @Test
    void should_reach_reconciliation_required_after_pending() {
        Payment payment = riskApproved();
        payment.markRailSubmitted();
        payment.markSettlementPending();
        payment.markReconciliationRequired();
        assertThat(payment.status()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);
    }

    @Test
    void should_forbid_settled_from_rail_submitted_without_pending() {
        Payment payment = riskApproved();
        payment.markRailSubmitted();
        assertThatThrownBy(payment::markSettled).isInstanceOf(IllegalPaymentStateException.class);
    }

    @Test
    void should_forbid_failed_final_from_settlement_pending() {
        Payment payment = riskApproved();
        payment.markRailSubmitted();
        payment.markSettlementPending();
        assertThatThrownBy(payment::markFailedFinal).isInstanceOf(IllegalPaymentStateException.class);
    }

    @Test
    void should_forbid_negative_money() {
        assertThatThrownBy(() -> Money.of("-1.00", "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Payment sample() {
        return Payment.create(UUID.randomUUID(), UUID.randomUUID(), Money.of("10.00", "USD"), "ord-1");
    }

    private static Payment riskApproved() {
        Payment payment = sample();
        payment.markRiskPending();
        payment.approveRisk();
        return payment;
    }
}
