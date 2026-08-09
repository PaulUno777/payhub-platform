package com.payhub.orchestrator.infrastructure.temporal;

import java.time.Duration;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

/**
 * Rail-first refund then FinLedger POST .../refunds (plan §4.3).
 */
public class RefundWorkflowImpl implements RefundWorkflow {

    private final RefundActivities activities = Workflow.newActivityStub(
            RefundActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)
                            .build())
                    .build()
    );

    @Override
    public void refund(RefundInput input) {
        activities.markRequested(input.paymentId(), input.refundAmount());

        RefundActivities.RefundRailOutcome psp = activities.submitToPsp(
                input.paymentId(),
                input.refundAmount(),
                input.sandboxMode()
        );

        if ("REJECTED".equals(psp.outcome())) {
            activities.failAfterPspReject(input.paymentId());
            return;
        }
        if ("AMBIGUOUS".equals(psp.outcome())) {
            activities.requireReconciliation(input.paymentId());
            return;
        }

        activities.markSettlementPendingAfterRailAccept(input.paymentId());
        String proof = activities.awaitFinalProof(
                input.paymentId(),
                psp.providerReference(),
                input.sandboxMode()
        );
        if ("AMBIGUOUS".equals(proof) || "REJECTED".equals(proof)) {
            activities.requireReconciliation(input.paymentId());
            return;
        }

        activities.confirmLedgerRefund(input.paymentId(), input.refundAmount());
    }
}
