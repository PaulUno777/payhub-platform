package com.payhub.orchestrator.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.payhub.orchestrator.TestcontainersConfiguration;
import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.domain.Money;
import com.payhub.orchestrator.domain.Payment;
import com.payhub.orchestrator.domain.PaymentStatus;

@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentRepositoryIT {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void should_round_trip_payment_through_risk_approved() {
        Payment created = Payment.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of("12.50", "USD"),
                "ord-it-1"
        );
        Payment saved = paymentRepository.save(created);

        Payment loaded = paymentRepository.findById(saved.id()).orElseThrow();
        assertThat(loaded.status()).isEqualTo(PaymentStatus.CREATED);

        loaded.markRiskPending();
        loaded.approveRisk();
        paymentRepository.save(loaded);

        Payment approved = paymentRepository.findById(saved.id()).orElseThrow();
        assertThat(approved.status()).isEqualTo(PaymentStatus.RISK_APPROVED);
        assertThat(approved.money().amount()).isEqualByComparingTo("12.50");
        assertThat(approved.clientReference()).isEqualTo("ord-it-1");
    }
}
