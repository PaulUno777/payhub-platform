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
import com.payhub.orchestrator.application.port.out.LedgerPort.ConfirmSettlementResult;
import com.payhub.orchestrator.application.port.out.LedgerPort.InitiateRailPaymentResult;
import com.payhub.orchestrator.application.port.out.MerchantPort;
import com.payhub.orchestrator.application.port.out.MerchantPort.MerchantSnapshot;
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
class CapturePaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private MerchantPort merchantPort;
    @Mock
    private RailPort railPort;
    @Mock
    private LedgerPort ledgerPort;
    @Mock
    private LedgerCredentialsPort ledgerCredentialsPort;
    @Mock
    private PaymentLifecyclePublisher paymentLifecyclePublisher;

    private CapturePaymentService service;
    private final Map<UUID, Payment> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new CapturePaymentService(
                paymentRepository,
                merchantPort,
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
    void should_fail_final_without_finledger_when_psp_rejects() {
        Payment payment = riskApproved();
        store.put(payment.id(), payment);
        when(railPort.submit(any())).thenReturn(new RailSubmitResult(RailResult.REJECTED, null));

        var psp = service.submitToPsp(payment.id(), "REJECT");
        assertThat(psp.outcome()).isEqualTo(RailResult.REJECTED);
        service.failAfterPspReject(payment.id());

        assertThat(store.get(payment.id()).status()).isEqualTo(PaymentStatus.FAILED_FINAL);
        verify(ledgerPort, never()).initiateRailPayment(any());
        verify(ledgerPort, never()).confirmSettlement(any());
        verify(paymentLifecyclePublisher).publishStatusChanged(any(), any(), any(), eq(PaymentStatus.RAIL_SUBMITTED));
        verify(paymentLifecyclePublisher).publishStatusChanged(any(), any(), any(), eq(PaymentStatus.FAILED_FINAL));
    }

    @Test
    void should_require_reconciliation_when_psp_ambiguous_before_pending() {
        Payment payment = riskApproved();
        store.put(payment.id(), payment);
        when(railPort.submit(any())).thenReturn(new RailSubmitResult(RailResult.AMBIGUOUS, null));

        var psp = service.submitToPsp(payment.id(), "AMBIGUOUS");
        assertThat(psp.outcome()).isEqualTo(RailResult.AMBIGUOUS);
        service.requireReconciliation(payment.id());

        assertThat(store.get(payment.id()).status()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);
        verify(ledgerPort, never()).initiateRailPayment(any());
        verify(ledgerPort, never()).confirmSettlement(any());
    }

    @Test
    void should_settle_on_happy_path_after_psp_accept() {
        Payment payment = riskApproved();
        store.put(payment.id(), payment);
        when(railPort.submit(any())).thenReturn(new RailSubmitResult(RailResult.ACCEPTED, "mm-1"));
        stubLedgerCredentials();
        when(ledgerPort.initiateRailPayment(any())).thenReturn(new InitiateRailPaymentResult(
                UUID.randomUUID(), "rail-ref-1", "INITIATED", UUID.randomUUID(), false
        ));
        when(railPort.awaitFinalProof(any())).thenReturn(new RailProofResult(RailResult.ACCEPTED));
        when(ledgerPort.confirmSettlement(any())).thenReturn(new ConfirmSettlementResult(
                "rail-ref-1", "SETTLED", false
        ));

        assertThat(service.submitToPsp(payment.id(), "ACCEPT").outcome()).isEqualTo(RailResult.ACCEPTED);
        assertThat(service.initiateAfterPspAccept(payment.id()).railReference()).isEqualTo("rail-ref-1");
        assertThat(service.awaitFinalProof(payment.id(), "mm-1", "ACCEPT")).isEqualTo(RailResult.ACCEPTED);
        service.settleAfterProof(payment.id(), "rail-ref-1");

        assertThat(store.get(payment.id()).status()).isEqualTo(PaymentStatus.SETTLED);
        verify(ledgerPort).initiateRailPayment(any());
        verify(ledgerPort).confirmSettlement(any());
        verify(paymentLifecyclePublisher).publishStatusChanged(any(), any(), any(), eq(PaymentStatus.SETTLED));
    }

    @Test
    void should_require_reconciliation_when_proof_ambiguous_after_pending() {
        Payment payment = riskApproved();
        store.put(payment.id(), payment);
        when(railPort.submit(any())).thenReturn(new RailSubmitResult(RailResult.ACCEPTED, "mm-1"));
        stubLedgerCredentials();
        when(ledgerPort.initiateRailPayment(any())).thenReturn(new InitiateRailPaymentResult(
                UUID.randomUUID(), "rail-ref-1", "INITIATED", UUID.randomUUID(), false
        ));
        when(railPort.awaitFinalProof(any())).thenReturn(new RailProofResult(RailResult.AMBIGUOUS));

        service.submitToPsp(payment.id(), "PROOF_AMBIGUOUS");
        service.initiateAfterPspAccept(payment.id());
        assertThat(service.awaitFinalProof(payment.id(), "mm-1", "PROOF_AMBIGUOUS"))
                .isEqualTo(RailResult.AMBIGUOUS);
        service.requireReconciliation(payment.id());

        assertThat(store.get(payment.id()).status()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);
        verify(ledgerPort, never()).confirmSettlement(any());
    }

    @Test
    void should_select_split_rule_key_from_merchant_port() {
        UUID merchantId = UUID.randomUUID();
        when(merchantPort.findById(merchantId))
                .thenReturn(new MerchantSnapshot(merchantId, "ecopay-default", null));
        assertThat(service.selectSplitRuleKey(merchantId)).isEqualTo("ecopay-default");
    }

    private void stubLedgerCredentials() {
        when(ledgerCredentialsPort.railCode()).thenReturn("MANUAL");
        when(ledgerCredentialsPort.bearerToken()).thenReturn("token");
        when(ledgerCredentialsPort.clearingAccountId()).thenReturn(UUID.randomUUID());
        when(ledgerCredentialsPort.counterpartyAccountId()).thenReturn(UUID.randomUUID());
    }

    private static Payment riskApproved() {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of("10.00", "USD"),
                "ord-1"
        );
        payment.markRiskPending();
        payment.approveRisk();
        return payment;
    }
}
