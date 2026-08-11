package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface(namePrefix = "PaymentCapture_")
public interface PaymentCaptureActivities {

    @ActivityMethod
    String selectSplitRuleKey(UUID merchantId);

    @ActivityMethod
    PspSubmitOutcome submitToPsp(UUID paymentId, String sandboxMode);

    @ActivityMethod
    void failAfterPspReject(UUID paymentId);

    @ActivityMethod
    void requireReconciliation(UUID paymentId);

    @ActivityMethod
    String initiateAfterPspAccept(UUID paymentId);

    @ActivityMethod
    String awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode);

    @ActivityMethod
    void settleAfterProof(UUID paymentId, String railReference);

    record PspSubmitOutcome(String outcome, String providerReference) {
    }
}
