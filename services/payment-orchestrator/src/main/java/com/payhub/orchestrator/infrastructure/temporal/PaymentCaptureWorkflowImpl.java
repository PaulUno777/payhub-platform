package com.payhub.orchestrator.infrastructure.temporal;

import java.time.Duration;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

/**
 * After RISK_APPROVED signal: SelectSplitRuleKey → SubmitToPSP → initiate → proof → settle.
 */
public class PaymentCaptureWorkflowImpl implements PaymentCaptureWorkflow {

    private boolean proceed;

    private final PaymentCaptureActivities activities = Workflow.newActivityStub(
            PaymentCaptureActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)
                            .build())
                    .build()
    );

    @Override
    public void capture(PaymentCaptureInput input) {
        Workflow.await(() -> proceed);

        activities.selectSplitRuleKey(input.merchantId());

        String sandboxMode = input.sandboxMode();
        PaymentCaptureActivities.PspSubmitOutcome psp = activities.submitToPsp(input.paymentId(), sandboxMode);

        if ("REJECTED".equals(psp.outcome())) {
            activities.failAfterPspReject(input.paymentId());
            return;
        }
        if ("AMBIGUOUS".equals(psp.outcome())) {
            activities.requireReconciliation(input.paymentId());
            return;
        }

        String railReference = activities.initiateAfterPspAccept(input.paymentId());
        String proof = activities.awaitFinalProof(input.paymentId(), psp.providerReference(), sandboxMode);
        if ("AMBIGUOUS".equals(proof) || "REJECTED".equals(proof)) {
            activities.requireReconciliation(input.paymentId());
            return;
        }

        activities.settleAfterProof(input.paymentId(), railReference);
    }

    @Override
    public void continueCapture() {
        proceed = true;
    }
}
