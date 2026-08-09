package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface RefundActivities {

    @ActivityMethod
    void markRequested(UUID paymentId, String refundAmount);

    @ActivityMethod
    RefundRailOutcome submitToPsp(UUID paymentId, String refundAmount, String sandboxMode);

    @ActivityMethod
    void failAfterPspReject(UUID paymentId);

    @ActivityMethod
    void requireReconciliation(UUID paymentId);

    @ActivityMethod
    void markSettlementPendingAfterRailAccept(UUID paymentId);

    @ActivityMethod
    String awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode);

    @ActivityMethod
    void confirmLedgerRefund(UUID paymentId, String refundAmount);

    record RefundRailOutcome(String outcome, String providerReference) {
    }
}
