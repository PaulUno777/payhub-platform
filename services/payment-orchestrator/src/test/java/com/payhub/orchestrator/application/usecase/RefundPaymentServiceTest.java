package com.payhub.orchestrator.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.orchestrator.application.port.out.LedgerCredentialsPort;
import com.payhub.orchestrator.application.port.out.LedgerPort;
import com.payhub.orchestrator.application.port.out.LedgerPort.RefundResult;
import com.payhub.orchestrator.application.port.out.PaymentLifecyclePublisher;
import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.application.port.out.RailPort;
import com.payhub.orchestrator.application.port.out.RailPort.RailProofResult;
import com.payhub.orchestrator.application.port.out.RailPort.RailResult;
import com.payhub.orchestrator.application.port.out.RailPort.RailSubmitResult;
import com.payhub.orchestrator.domain.Money;
import com.payhub.orchestrator.domain.Payment;
import com.payhub.orchestrator.domain.PaymentStatus;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RefundPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RailPort railPort;
    @Mock
    private LedgerPort ledgerPort;
    @Mock
    private LedgerCredentialsPort ledgerCredentialsPort;
    @Mock
    private PaymentLifecyclePublisher paymentLifecyclePublisher;

    private RefundPaymentService service;
    private final Map<UUID, Payment> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new RefundPaymentService(
                paymentRepository,
                railPort,
                ledgerPort,
                ledgerCredentialsPort,
                paymentLifecyclePublisher
        );
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            store.put(p.id(), p);
            return p;
        });
        lenient().when(paymentRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
    }

    @Test
    void should_fail_refund_without_finledger_when_psp_rejects() {
        Payment payment = settled();
        store.put(payment.id(), payment);
        when(railPort.submitRefund(any())).thenReturn(new RailSubmitResult(RailResult.REJECTED, null));

        service.markRequested(payment.id(), "4.00");
        assertThat(service.submitToPsp(payment.id(), "4.00", "REJECT").outcome())
                .isEqualTo(RailResult.REJECTED);
        service.failAfterPspReject(payment.id());

        assertThat(store.get(payment.id()).status()).isEqualTo(PaymentStatus.REFUND_FAILED_FINAL);
        verify(ledgerPort, never()).refund(any());
    }

    @Test
    void should_require_reconciliation_when_psp_ambiguous() {
        Payment payment = settled();
        store.put(payment.id(), payment);
        when(railPort.submitRefund(any())).thenReturn(new RailSubmitResult(RailResult.AMBIGUOUS, null));

        service.markRequested(payment.id(), "4.00");
        service.submitToPsp(payment.id(), "4.00", "AMBIGUOUS");
        service.requireReconciliation(payment.id());

        assertThat(store.get(payment.id()).status()).isEqualTo(PaymentStatus.REFUND_RECONCILIATION_REQUIRED);
        verify(ledgerPort, never()).refund(any());
    }

    @Test
    void should_partial_refund_pass_through_amount_to_ledger_without_fee_math() {
        Payment payment = settled();
        store.put(payment.id(), payment);
        when(railPort.submitRefund(any())).thenReturn(new RailSubmitResult(RailResult.ACCEPTED, "mm-refund-1"));
        when(railPort.awaitRefundProof(any())).thenReturn(new RailProofResult(RailResult.ACCEPTED));
        when(ledgerCredentialsPort.bearerToken()).thenReturn("token");
        when(ledgerPort.refund(any())).thenReturn(new RefundResult(UUID.randomUUID(), "SETTLED", false));

        service.markRequested(payment.id(), "4.00");
        service.submitToPsp(payment.id(), "4.00", "ACCEPT");
        service.markSettlementPendingAfterRailAccept(payment.id());
        assertThat(service.awaitFinalProof(payment.id(), "mm-refund-1", "ACCEPT"))
                .isEqualTo(RailResult.ACCEPTED);
        service.confirmLedgerRefund(payment.id(), "4.00");

        assertThat(store.get(payment.id()).status()).isEqualTo(PaymentStatus.REFUND_SETTLED);
        assertThat(store.get(payment.id()).refundedAmount()).isEqualByComparingTo("4.00");
        verify(ledgerPort).refund(any());
        verify(paymentLifecyclePublisher).publishStatusChanged(any(), any(), any(), eq(PaymentStatus.REFUND_SETTLED));
    }

    private static Payment settled() {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of("10.00", "USD"),
                "ord-1"
        );
        payment.markRiskPending();
        payment.approveRisk();
        payment.markRailSubmitted();
        payment.markSettlementPending("rail-1", UUID.randomUUID());
        payment.markSettled();
        return payment;
    }
}
