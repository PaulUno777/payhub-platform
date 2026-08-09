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
    void should_forbid_negative_money() {
        assertThatThrownBy(() -> Money.of("-1.00", "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Payment sample() {
        return Payment.create(UUID.randomUUID(), UUID.randomUUID(), Money.of("10.00", "USD"), "ord-1");
    }
}
