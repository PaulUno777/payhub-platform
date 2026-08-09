package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.in.CapturePaymentUseCase;

@Component
public class PaymentCaptureActivitiesImpl implements PaymentCaptureActivities {

    private final CapturePaymentUseCase capturePaymentUseCase;

    public PaymentCaptureActivitiesImpl(CapturePaymentUseCase capturePaymentUseCase) {
        this.capturePaymentUseCase = capturePaymentUseCase;
    }

    @Override
    public String selectSplitRuleKey(UUID merchantId) {
        return capturePaymentUseCase.selectSplitRuleKey(merchantId);
    }

    @Override
    public PspSubmitOutcome submitToPsp(UUID paymentId, String sandboxMode) {
        var result = capturePaymentUseCase.submitToPsp(paymentId, sandboxMode);
        return new PspSubmitOutcome(result.outcome().name(), result.providerReference());
    }

    @Override
    public void failAfterPspReject(UUID paymentId) {
        capturePaymentUseCase.failAfterPspReject(paymentId);
    }

    @Override
    public void requireReconciliation(UUID paymentId) {
        capturePaymentUseCase.requireReconciliation(paymentId);
    }

    @Override
    public String initiateAfterPspAccept(UUID paymentId) {
        return capturePaymentUseCase.initiateAfterPspAccept(paymentId).railReference();
    }

    @Override
    public String awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode) {
        return capturePaymentUseCase.awaitFinalProof(paymentId, providerReference, sandboxMode).name();
    }

    @Override
    public void settleAfterProof(UUID paymentId, String railReference) {
        capturePaymentUseCase.settleAfterProof(paymentId, railReference);
    }
}
