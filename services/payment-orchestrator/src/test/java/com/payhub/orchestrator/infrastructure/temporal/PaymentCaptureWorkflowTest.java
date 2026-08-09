package com.payhub.orchestrator.infrastructure.temporal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

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
    private RecordingActivities activities;

    @BeforeEach
    void setUp() {
        environment = TestWorkflowEnvironment.newInstance();
        Worker worker = environment.newWorker("payment-capture");
        worker.registerWorkflowImplementationTypes(PaymentCaptureWorkflowImpl.class);
        activities = new RecordingActivities();
        worker.registerActivitiesImplementations(activities);
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
    void should_park_until_continue_then_settle_on_accept() throws TimeoutException {
        UUID paymentId = UUID.randomUUID();
        activities.submitOutcome.set(new PaymentCaptureActivities.PspSubmitOutcome("ACCEPTED", "mm-1"));
        activities.proofOutcome.set("ACCEPTED");

        PaymentCaptureWorkflow stub = newStub(paymentId, "ACCEPT");
        WorkflowClient.start(stub::capture, input(paymentId, "ACCEPT"));

        WorkflowStub untyped = WorkflowStub.fromTyped(stub);
        assertThat(untyped.describe().getStatus())
                .isEqualTo(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING);

        stub.continueCapture();
        untyped.getResult(5, TimeUnit.SECONDS, Void.class);

        assertThat(activities.calls).contains("settle:" + paymentId + ":rail-ref-1");
        assertThat(activities.calls).noneMatch(c -> c.startsWith("fail:") || c.startsWith("reconcile:"));
    }

    @Test
    void should_fail_final_without_initiate_when_psp_rejects() throws TimeoutException {
        UUID paymentId = UUID.randomUUID();
        activities.submitOutcome.set(new PaymentCaptureActivities.PspSubmitOutcome("REJECTED", null));

        PaymentCaptureWorkflow stub = newStub(paymentId, "REJECT");
        WorkflowClient.start(stub::capture, input(paymentId, "REJECT"));
        stub.continueCapture();
        WorkflowStub.fromTyped(stub).getResult(5, TimeUnit.SECONDS, Void.class);

        assertThat(activities.calls).contains("fail:" + paymentId);
        assertThat(activities.calls).noneMatch(c -> c.startsWith("initiate:") || c.startsWith("settle:"));
    }

    @Test
    void should_require_reconciliation_when_psp_ambiguous() throws TimeoutException {
        UUID paymentId = UUID.randomUUID();
        activities.submitOutcome.set(new PaymentCaptureActivities.PspSubmitOutcome("AMBIGUOUS", null));

        PaymentCaptureWorkflow stub = newStub(paymentId, "AMBIGUOUS");
        WorkflowClient.start(stub::capture, input(paymentId, "AMBIGUOUS"));
        stub.continueCapture();
        WorkflowStub.fromTyped(stub).getResult(5, TimeUnit.SECONDS, Void.class);

        assertThat(activities.calls).contains("reconcile:" + paymentId);
        assertThat(activities.calls).noneMatch(c -> c.startsWith("initiate:") || c.startsWith("settle:"));
    }

    private PaymentCaptureWorkflow newStub(UUID paymentId, String mode) {
        return client.newWorkflowStub(
                PaymentCaptureWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("payment-capture")
                        .setWorkflowId("payment-capture-" + paymentId + "-" + mode)
                        .setWorkflowRunTimeout(Duration.ofMinutes(1))
                        .build()
        );
    }

    private static PaymentCaptureWorkflow.PaymentCaptureInput input(UUID paymentId, String mode) {
        return new PaymentCaptureWorkflow.PaymentCaptureInput(
                paymentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "10.00",
                "USD",
                "ord-wf-1",
                mode
        );
    }

    static final class RecordingActivities implements PaymentCaptureActivities {
        final List<String> calls = new ArrayList<>();
        final AtomicReference<PaymentCaptureActivities.PspSubmitOutcome> submitOutcome =
                new AtomicReference<>(new PaymentCaptureActivities.PspSubmitOutcome("ACCEPTED", "mm-1"));
        final AtomicReference<String> proofOutcome = new AtomicReference<>("ACCEPTED");

        @Override
        public String selectSplitRuleKey(UUID merchantId) {
            calls.add("select:" + merchantId);
            return "ecopay-default";
        }

        @Override
        public PaymentCaptureActivities.PspSubmitOutcome submitToPsp(UUID paymentId, String sandboxMode) {
            calls.add("submit:" + paymentId + ":" + sandboxMode);
            return submitOutcome.get();
        }

        @Override
        public void failAfterPspReject(UUID paymentId) {
            calls.add("fail:" + paymentId);
        }

        @Override
        public void requireReconciliation(UUID paymentId) {
            calls.add("reconcile:" + paymentId);
        }

        @Override
        public String initiateAfterPspAccept(UUID paymentId) {
            calls.add("initiate:" + paymentId);
            return "rail-ref-1";
        }

        @Override
        public String awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode) {
            calls.add("proof:" + paymentId);
            return proofOutcome.get();
        }

        @Override
        public void settleAfterProof(UUID paymentId, String railReference) {
            calls.add("settle:" + paymentId + ":" + railReference);
        }
    }
}
