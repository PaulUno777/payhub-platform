package com.payhub.orchestrator.infrastructure.temporal;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.WorkflowPort;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

@Component
@ConditionalOnProperty(prefix = "payhub.temporal", name = "enabled", havingValue = "true")
public class TemporalWorkflowPort implements WorkflowPort {

    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;

    public TemporalWorkflowPort(WorkflowClient workflowClient, TemporalProperties properties) {
        this.workflowClient = workflowClient;
        this.properties = properties;
    }

    @Override
    public void startPaymentCapture(PaymentCaptureStart command) {
        PaymentCaptureWorkflow workflow = workflowClient.newWorkflowStub(
                PaymentCaptureWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(properties.taskQueue())
                        .setWorkflowId("payment-capture-" + command.paymentId())
                        .build()
        );
        WorkflowClient.start(
                workflow::capture,
                new PaymentCaptureWorkflow.PaymentCaptureInput(
                        command.paymentId(),
                        command.merchantId(),
                        command.tenantId(),
                        command.amount(),
                        command.currencyCode(),
                        command.clientReference(),
                        command.sandboxMode()
                )
        );
    }

    @Override
    public void signalContinueCapture(UUID paymentId) {
        PaymentCaptureWorkflow workflow = workflowClient.newWorkflowStub(
                PaymentCaptureWorkflow.class,
                "payment-capture-" + paymentId
        );
        workflow.continueCapture();
    }
}
