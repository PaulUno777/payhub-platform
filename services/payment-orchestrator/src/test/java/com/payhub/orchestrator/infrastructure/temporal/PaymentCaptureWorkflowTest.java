package com.payhub.orchestrator.infrastructure.temporal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

@Tag("unit")
class PaymentCaptureWorkflowTest {

    private TestWorkflowEnvironment environment;
    private WorkflowClient client;

    @BeforeEach
    void setUp() {
        environment = TestWorkflowEnvironment.newInstance();
        Worker worker = environment.newWorker("payment-capture");
        worker.registerWorkflowImplementationTypes(PaymentCaptureWorkflowImpl.class);
        client = environment.getWorkflowClient();
        environment.start();
    }

    @AfterEach
    void tearDown() {
        if (environment != null) {
            environment.close();
        }
    }

    @Test
    void should_start_workflow_for_payment_id_and_park_until_continue_signal() throws TimeoutException {
        UUID paymentId = UUID.randomUUID();
        PaymentCaptureWorkflow stub = client.newWorkflowStub(
                PaymentCaptureWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("payment-capture")
                        .setWorkflowId("payment-capture-" + paymentId)
                        .setWorkflowRunTimeout(Duration.ofMinutes(1))
                        .build()
        );

        WorkflowClient.start(
                stub::capture,
                new PaymentCaptureWorkflow.PaymentCaptureInput(
                        paymentId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "10.00",
                        "USD",
                        "ord-wf-1"
                )
        );

        WorkflowStub untyped = WorkflowStub.fromTyped(stub);
        assertThat(untyped.describe().getStatus())
                .isEqualTo(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING);

        stub.continueCapture();
        untyped.getResult(5, TimeUnit.SECONDS, Void.class);

        assertThat(untyped.describe().getStatus())
                .isEqualTo(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_COMPLETED);
    }
}
