package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PaymentCaptureWorkflow {

    @WorkflowMethod
    void capture(PaymentCaptureInput input);

    /** Resume after RISK_APPROVED (sync risk on HTTP path). */
    @SignalMethod
    void continueCapture();

    record PaymentCaptureInput(
            UUID paymentId,
            UUID merchantId,
            UUID tenantId,
            String amount,
            String currencyCode,
            String clientReference,
            String sandboxMode
    ) {
        public PaymentCaptureInput {
            if (sandboxMode == null || sandboxMode.isBlank()) {
                sandboxMode = "ACCEPT";
            }
        }
    }
}
