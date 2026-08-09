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

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

@Tag("unit")
class RefundWorkflowTest {

    private TestWorkflowEnvironment environment;
    private WorkflowClient client;
    private RecordingActivities activities;

    @BeforeEach
    void setUp() {
        environment = TestWorkflowEnvironment.newInstance();
        Worker worker = environment.newWorker("payment-capture");
        worker.registerWorkflowImplementationTypes(RefundWorkflowImpl.class);
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
    void should_settle_partial_refund_on_accept() throws TimeoutException {
        UUID paymentId = UUID.randomUUID();
        activities.submitOutcome.set(new RefundActivities.RefundRailOutcome("ACCEPTED", "mm-r-1"));
        activities.proofOutcome.set("ACCEPTED");

        RefundWorkflow stub = newStub(paymentId, "ACCEPT");
        WorkflowClient.start(stub::refund, new RefundWorkflow.RefundInput(paymentId, "4.00", "ACCEPT"));
        WorkflowStub.fromTyped(stub).getResult(5, TimeUnit.SECONDS, Void.class);

        assertThat(activities.calls).contains("ledger:" + paymentId + ":4.00");
        assertThat(activities.calls).noneMatch(c -> c.startsWith("fail:") || c.startsWith("reconcile:"));
    }

    @Test
    void should_fail_without_ledger_when_psp_rejects() throws TimeoutException {
        UUID paymentId = UUID.randomUUID();
        activities.submitOutcome.set(new RefundActivities.RefundRailOutcome("REJECTED", null));

        RefundWorkflow stub = newStub(paymentId, "REJECT");
        WorkflowClient.start(stub::refund, new RefundWorkflow.RefundInput(paymentId, "4.00", "REJECT"));
        WorkflowStub.fromTyped(stub).getResult(5, TimeUnit.SECONDS, Void.class);

        assertThat(activities.calls).contains("fail:" + paymentId);
        assertThat(activities.calls).noneMatch(c -> c.startsWith("ledger:"));
    }

    @Test
    void should_reconcile_when_psp_ambiguous() throws TimeoutException {
        UUID paymentId = UUID.randomUUID();
        activities.submitOutcome.set(new RefundActivities.RefundRailOutcome("AMBIGUOUS", null));

        RefundWorkflow stub = newStub(paymentId, "AMBIGUOUS");
        WorkflowClient.start(stub::refund, new RefundWorkflow.RefundInput(paymentId, "4.00", "AMBIGUOUS"));
        WorkflowStub.fromTyped(stub).getResult(5, TimeUnit.SECONDS, Void.class);

        assertThat(activities.calls).contains("reconcile:" + paymentId);
        assertThat(activities.calls).noneMatch(c -> c.startsWith("ledger:"));
    }

    private RefundWorkflow newStub(UUID paymentId, String mode) {
        return client.newWorkflowStub(
                RefundWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("payment-capture")
                        .setWorkflowId("payment-refund-" + paymentId + "-" + mode)
                        .setWorkflowRunTimeout(Duration.ofMinutes(1))
                        .build()
        );
    }

    static final class RecordingActivities implements RefundActivities {
        final List<String> calls = new ArrayList<>();
        final AtomicReference<RefundActivities.RefundRailOutcome> submitOutcome =
                new AtomicReference<>(new RefundActivities.RefundRailOutcome("ACCEPTED", "mm-r-1"));
        final AtomicReference<String> proofOutcome = new AtomicReference<>("ACCEPTED");

        @Override
        public void markRequested(UUID paymentId, String refundAmount) {
            calls.add("requested:" + paymentId);
        }

        @Override
        public RefundRailOutcome submitToPsp(UUID paymentId, String refundAmount, String sandboxMode) {
            calls.add("submit:" + paymentId);
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
        public void markSettlementPendingAfterRailAccept(UUID paymentId) {
            calls.add("pending:" + paymentId);
        }

        @Override
        public String awaitFinalProof(UUID paymentId, String providerReference, String sandboxMode) {
            calls.add("proof:" + paymentId);
            return proofOutcome.get();
        }

        @Override
        public void confirmLedgerRefund(UUID paymentId, String refundAmount) {
            calls.add("ledger:" + paymentId + ":" + refundAmount);
        }
    }
}
