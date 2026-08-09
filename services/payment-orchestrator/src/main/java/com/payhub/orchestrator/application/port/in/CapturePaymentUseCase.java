package com.payhub.orchestrator.application.port.in;

import java.util.UUID;

import com.payhub.orchestrator.application.port.out.RailPort.RailResult;

/**
 * Async capture steps after RISK_APPROVED (Temporal activities).
 */
public interface CapturePaymentUseCase {

    String selectSplitRuleKey(UUID merchantId);

    SubmitPspResult submitToPsp(UUID paymentId, String sandboxMode);

    void failAfterPspReject(UUID paymentId);

    void requireReconciliation(UUID paymentId);

    InitiateResult initiateAfterPspAccept(UUID paymentId);

    RailResult awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode);

    void settleAfterProof(UUID paymentId, String railReference);

    record SubmitPspResult(RailResult outcome, String providerReference) {
    }

    record InitiateResult(String railReference) {
    }
}
