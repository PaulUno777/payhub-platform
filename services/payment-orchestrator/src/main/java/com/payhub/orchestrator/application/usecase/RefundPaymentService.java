package com.payhub.orchestrator.application.usecase;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.orchestrator.application.IllegalPaymentTransitionException;
import com.payhub.orchestrator.application.PaymentNotFoundException;
import com.payhub.orchestrator.application.port.in.RefundPaymentUseCase;
import com.payhub.orchestrator.application.port.out.LedgerCredentialsPort;
import com.payhub.orchestrator.application.port.out.LedgerPort;
import com.payhub.orchestrator.application.port.out.LedgerPort.RefundCommand;
import com.payhub.orchestrator.application.port.out.PaymentLifecyclePublisher;
import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.application.port.out.RailPort;
import com.payhub.orchestrator.application.port.out.RailPort.RailProofCommand;
import com.payhub.orchestrator.application.port.out.RailPort.RailResult;
import com.payhub.orchestrator.application.port.out.RailPort.RailSubmitCommand;
import com.payhub.orchestrator.domain.IllegalPaymentStateException;
import com.payhub.orchestrator.domain.Payment;

@Service
public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final RailPort railPort;
    private final LedgerPort ledgerPort;
    private final LedgerCredentialsPort ledgerCredentialsPort;
    private final PaymentLifecyclePublisher paymentLifecyclePublisher;

    public RefundPaymentService(
            PaymentRepository paymentRepository,
            RailPort railPort,
            LedgerPort ledgerPort,
            LedgerCredentialsPort ledgerCredentialsPort,
            PaymentLifecyclePublisher paymentLifecyclePublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.railPort = railPort;
        this.ledgerPort = ledgerPort;
        this.ledgerCredentialsPort = ledgerCredentialsPort;
        this.paymentLifecyclePublisher = paymentLifecyclePublisher;
    }

    @Override
    @Transactional
    public void markRequested(UUID paymentId, String refundAmount) {
        Payment payment = require(paymentId);
        try {
            payment.requestRefund(new BigDecimal(refundAmount));
        } catch (IllegalPaymentStateException | IllegalArgumentException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);
    }

    @Override
    @Transactional
    public SubmitRefundRailResult submitToPsp(UUID paymentId, String refundAmount, String sandboxMode) {
        Payment payment = require(paymentId);
        try {
            payment.markRefundRailSubmitted();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);

        var result = railPort.submitRefund(new RailSubmitCommand(
                payment.id(),
                refundAmount,
                payment.money().currency().getCurrencyCode(),
                sandboxMode
        ));
        return new SubmitRefundRailResult(result.outcome(), result.providerReference());
    }

    @Override
    @Transactional
    public void failAfterPspReject(UUID paymentId) {
        Payment payment = require(paymentId);
        try {
            payment.markRefundFailedFinal();
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
            payment.markRefundReconciliationRequired();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);
    }

    @Override
    @Transactional
    public void markSettlementPendingAfterRailAccept(UUID paymentId) {
        Payment payment = require(paymentId);
        try {
            payment.markRefundSettlementPending();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }
        persist(payment);
    }

    @Override
    public RailResult awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode) {
        return railPort.awaitRefundProof(new RailProofCommand(paymentId, providerReference, sandboxMode))
                .outcome();
    }

    @Override
    @Transactional
    public void confirmLedgerRefund(UUID paymentId, String refundAmount) {
        Payment payment = require(paymentId);
        if (payment.initiateJournalEntryId() == null || payment.railReference() == null) {
            throw new IllegalPaymentTransitionException("Payment missing ledger refs for refund");
        }

        ledgerPort.refund(new RefundCommand(
                payment.tenantId(),
                "refund-" + payment.id() + "-" + refundAmount,
                payment.railReference(),
                payment.initiateJournalEntryId(),
                refundAmount,
                payment.money().currency().getCurrencyCode(),
                Objects.requireNonNull(ledgerCredentialsPort.bearerToken(), "bearerToken")
        ));

        try {
            payment.markRefundSettled(new BigDecimal(refundAmount));
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
