package com.payhub.orchestrator.application.usecase;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.orchestrator.application.IllegalPaymentTransitionException;
import com.payhub.orchestrator.application.PaymentNotFoundException;
import com.payhub.orchestrator.application.port.in.CapturePaymentUseCase;
import com.payhub.orchestrator.application.port.out.LedgerCredentialsPort;
import com.payhub.orchestrator.application.port.out.LedgerPort;
import com.payhub.orchestrator.application.port.out.LedgerPort.ConfirmSettlementCommand;
import com.payhub.orchestrator.application.port.out.LedgerPort.InitiateRailPaymentCommand;
import com.payhub.orchestrator.application.port.out.MerchantPort;
import com.payhub.orchestrator.application.port.out.PaymentLifecyclePublisher;
import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.application.port.out.RailPort;
import com.payhub.orchestrator.application.port.out.RailPort.RailProofCommand;
import com.payhub.orchestrator.application.port.out.RailPort.RailResult;
import com.payhub.orchestrator.application.port.out.RailPort.RailSubmitCommand;
import com.payhub.orchestrator.domain.IllegalPaymentStateException;
import com.payhub.orchestrator.domain.Payment;
import com.payhub.orchestrator.domain.PaymentStatus;

@Service
public class CapturePaymentService implements CapturePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final MerchantPort merchantPort;
    private final RailPort railPort;
    private final LedgerPort ledgerPort;
    private final LedgerCredentialsPort ledgerCredentialsPort;
    private final PaymentLifecyclePublisher paymentLifecyclePublisher;

    public CapturePaymentService(
            PaymentRepository paymentRepository,
            MerchantPort merchantPort,
            RailPort railPort,
            LedgerPort ledgerPort,
            LedgerCredentialsPort ledgerCredentialsPort,
            PaymentLifecyclePublisher paymentLifecyclePublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.merchantPort = merchantPort;
        this.railPort = railPort;
        this.ledgerPort = ledgerPort;
        this.ledgerCredentialsPort = ledgerCredentialsPort;
        this.paymentLifecyclePublisher = paymentLifecyclePublisher;
    }

    @Override
    public String selectSplitRuleKey(UUID merchantId) {
        return merchantPort.findById(merchantId).assignedRuleSetKey();
    }

    @Override
    @Transactional
    public SubmitPspResult submitToPsp(UUID paymentId, String sandboxMode) {
        Payment payment = require(paymentId);
        try {
            payment.markRailSubmitted();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);

        var result = railPort.submit(new RailSubmitCommand(
                payment.id(),
                payment.money().amount().toPlainString(),
                payment.money().currency().getCurrencyCode(),
                sandboxMode
        ));
        return new SubmitPspResult(result.outcome(), result.providerReference());
    }

    @Override
    @Transactional
    public void failAfterPspReject(UUID paymentId) {
        Payment payment = require(paymentId);
        try {
            payment.markFailedFinal();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);
    }

    @Override
    @Transactional
    public void requireReconciliation(UUID paymentId) {
        Payment payment = require(paymentId);
        try {
            payment.markReconciliationRequired();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);
    }

    @Override
    @Transactional
    public InitiateResult initiateAfterPspAccept(UUID paymentId) {
        Payment payment = require(paymentId);
        if (payment.status() != PaymentStatus.RAIL_SUBMITTED) {
            throw new IllegalPaymentTransitionException(
                    "Cannot initiate ledger from status " + payment.status());
        }

        var initiated = ledgerPort.initiateRailPayment(new InitiateRailPaymentCommand(
                payment.tenantId(),
                "rail-init-" + payment.id(),
                ledgerCredentialsPort.railCode(),
                payment.money().amount().toPlainString(),
                payment.money().currency().getCurrencyCode(),
                ledgerCredentialsPort.clearingAccountId(),
                ledgerCredentialsPort.counterpartyAccountId(),
                payment.clientReference(),
                Objects.requireNonNull(ledgerCredentialsPort.bearerToken(), "bearerToken")
        ));

        try {
            payment.markSettlementPending();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);
        return new InitiateResult(initiated.railReference());
    }

    @Override
    public RailResult awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode) {
        return railPort.awaitFinalProof(new RailProofCommand(paymentId, providerReference, sandboxMode))
                .outcome();
    }

    @Override
    @Transactional
    public void settleAfterProof(UUID paymentId, String railReference) {
        Payment payment = require(paymentId);
        if (payment.status() != PaymentStatus.SETTLEMENT_PENDING) {
            throw new IllegalPaymentTransitionException(
                    "Cannot settle from status " + payment.status());
        }

        ledgerPort.confirmSettlement(new ConfirmSettlementCommand(
                payment.tenantId(),
                railReference,
                "rail-settle-" + payment.id(),
                Objects.requireNonNull(ledgerCredentialsPort.bearerToken(), "bearerToken")
        ));

        try {
            payment.markSettled();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);
    }

    private Payment require(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private void persist(Payment payment) {
        Payment saved = paymentRepository.save(payment);
        paymentLifecyclePublisher.publishStatusChanged(
                saved.id(),
                saved.merchantId(),
                saved.tenantId(),
                saved.status()
        );
    }
}
