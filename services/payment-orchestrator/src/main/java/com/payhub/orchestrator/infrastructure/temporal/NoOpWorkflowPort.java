package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.WorkflowPort;

/**
 * Used when Temporal is disabled (unit/boot smoke without Temporal server).
 */
@Component
@ConditionalOnProperty(prefix = "payhub.temporal", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpWorkflowPort implements WorkflowPort {

    @Override
    public void startPaymentCapture(PaymentCaptureStart command) {
        // no-op for local tests without Temporal
    }

    @Override
    public void signalContinueCapture(UUID paymentId) {
        // no-op
    }

    @Override
    public void startRefund(RefundStart command) {
        // no-op
    }
}
