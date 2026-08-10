package com.payhub.orchestrator.application.usecase;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.payhub.orchestrator.application.port.in.ContinueCaptureUseCase;
import com.payhub.orchestrator.application.port.out.WorkflowPort;

/**
 * Ops/lab signal to resume a parked PaymentCapture workflow (DS-019 kind failover).
 */
@Service
public class ContinueCaptureService implements ContinueCaptureUseCase {

    private final WorkflowPort workflowPort;

    public ContinueCaptureService(WorkflowPort workflowPort) {
        this.workflowPort = workflowPort;
    }

    @Override
    public void execute(UUID paymentId) {
        Objects.requireNonNull(paymentId, "paymentId");
        workflowPort.signalContinueCapture(paymentId);
    }
}
