package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PaymentCaptureWorkflow {

    @WorkflowMethod
    void capture(PaymentCaptureInput input);

    /** Reserved for DS-008+ to resume after RISK_APPROVED. */
    @SignalMethod
    void continueCapture();

    record PaymentCaptureInput(
            UUID paymentId,
            UUID merchantId,
            UUID tenantId,
            String amount,
            String currencyCode,
            String clientReference
    ) {
    }
}
