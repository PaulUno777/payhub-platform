package com.payhub.orchestrator.application.port.in;

import java.util.UUID;

import com.payhub.orchestrator.application.port.out.RailPort.RailResult;

public interface RefundPaymentUseCase {

    void markRequested(UUID paymentId, String refundAmount);

    SubmitRefundRailResult submitToPsp(UUID paymentId, String refundAmount, String sandboxMode);

    void failAfterPspReject(UUID paymentId);

    void requireReconciliation(UUID paymentId);

    void markSettlementPendingAfterRailAccept(UUID paymentId);

    RailResult awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode);

    void confirmLedgerRefund(UUID paymentId, String refundAmount);

    record SubmitRefundRailResult(RailResult outcome, String providerReference) {
    }
}
