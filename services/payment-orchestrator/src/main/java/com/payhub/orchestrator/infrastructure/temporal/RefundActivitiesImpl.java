package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.in.RefundPaymentUseCase;

@Component
public class RefundActivitiesImpl implements RefundActivities {

    private final RefundPaymentUseCase refundPaymentUseCase;

    public RefundActivitiesImpl(RefundPaymentUseCase refundPaymentUseCase) {
        this.refundPaymentUseCase = refundPaymentUseCase;
    }

    @Override
    public void markRequested(UUID paymentId, String refundAmount) {
        refundPaymentUseCase.markRequested(paymentId, refundAmount);
    }

    @Override
    public RefundRailOutcome submitToPsp(UUID paymentId, String refundAmount, String sandboxMode) {
        var result = refundPaymentUseCase.submitToPsp(paymentId, refundAmount, sandboxMode);
        return new RefundRailOutcome(result.outcome().name(), result.providerReference());
    }

    @Override
    public void failAfterPspReject(UUID paymentId) {
        refundPaymentUseCase.failAfterPspReject(paymentId);
    }

    @Override
    public void requireReconciliation(UUID paymentId) {
        refundPaymentUseCase.requireReconciliation(paymentId);
    }

    @Override
    public void markSettlementPendingAfterRailAccept(UUID paymentId) {
        refundPaymentUseCase.markSettlementPendingAfterRailAccept(paymentId);
    }

    @Override
    public String awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode) {
        return refundPaymentUseCase.awaitFinalProof(paymentId, providerReference, sandboxMode).name();
    }

    @Override
    public void confirmLedgerRefund(UUID paymentId, String refundAmount) {
        refundPaymentUseCase.confirmLedgerRefund(paymentId, refundAmount);
    }
}
