package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface RefundWorkflow {

    @WorkflowMethod
    void refund(RefundInput input);

    record RefundInput(UUID paymentId, String refundAmount, String sandboxMode) {
        public RefundInput {
            if (sandboxMode == null || sandboxMode.isBlank()) {
                sandboxMode = "ACCEPT";
            }
        }
    }
}
