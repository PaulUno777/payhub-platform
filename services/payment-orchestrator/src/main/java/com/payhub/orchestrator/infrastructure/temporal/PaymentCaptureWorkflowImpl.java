package com.payhub.orchestrator.infrastructure.temporal;

import io.temporal.workflow.Workflow;

/**
 * DS-006: workflow is started at create; sync risk runs in the use case.
 * Parks until continueCapture signal (wired in DS-008).
 */
public class PaymentCaptureWorkflowImpl implements PaymentCaptureWorkflow {

    private boolean proceed;

    @Override
    public void capture(PaymentCaptureInput input) {
        Workflow.await(() -> proceed);
    }

    @Override
    public void continueCapture() {
        proceed = true;
    }
}
