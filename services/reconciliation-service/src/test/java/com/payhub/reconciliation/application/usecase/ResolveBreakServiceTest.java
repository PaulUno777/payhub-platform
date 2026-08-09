package com.payhub.reconciliation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.payhub.reconciliation.application.dto.BreakView;
import com.payhub.reconciliation.application.port.in.ResolveBreakUseCase;
import com.payhub.reconciliation.application.port.out.BreakRepository;
import com.payhub.reconciliation.application.port.out.OrchestratorPaymentPort;
import com.payhub.reconciliation.domain.Break;
import com.payhub.reconciliation.domain.BreakStatus;
import com.payhub.reconciliation.domain.BreakType;
import com.payhub.reconciliation.domain.ResolveAction;

@Tag("unit")
class ResolveBreakServiceTest {

    @Test
    void should_resolve_confirm_with_audit_without_orchestrator_call() {
        Break brk = Break.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ext-1",
                BreakType.STATUS_MISMATCH,
                "mismatch"
        );
        InMemoryBreaks breaks = new InMemoryBreaks(brk);
        RecordingOrchestrator orch = new RecordingOrchestrator();

        ResolveBreakService service = new ResolveBreakService(breaks, orch);
        BreakView view = service.execute(new ResolveBreakUseCase.Command(
                brk.id(),
                "CONFIRM",
                "ops-alice",
                "idem-1"
        ));

        assertThat(view.status()).isEqualTo(BreakStatus.RESOLVED.name());
        assertThat(view.resolutionAction()).isEqualTo(ResolveAction.CONFIRM.name());
        assertThat(view.resolvedBy()).isEqualTo("ops-alice");
        assertThat(orch.lastAction.get()).isNull();
    }

    @Test
    void should_request_reversal_via_orchestrator_then_close_break() {
        UUID paymentId = UUID.randomUUID();
        Break brk = Break.open(
                UUID.randomUUID(),
                paymentId,
                "ext-2",
                BreakType.STATUS_MISMATCH,
                "orphan"
        );
        InMemoryBreaks breaks = new InMemoryBreaks(brk);
        RecordingOrchestrator orch = new RecordingOrchestrator();

        ResolveBreakService service = new ResolveBreakService(breaks, orch);
        BreakView view = service.execute(new ResolveBreakUseCase.Command(
                brk.id(),
                "REQUEST_REVERSAL",
                "ops-bob",
                "idem-2"
        ));

        assertThat(view.status()).isEqualTo(BreakStatus.RESOLVED.name());
        assertThat(view.resolutionAction()).isEqualTo(ResolveAction.REQUEST_REVERSAL.name());
        assertThat(orch.lastAction.get()).isEqualTo("REQUEST_REVERSAL");
        assertThat(orch.lastPaymentId.get()).isEqualTo(paymentId);
    }

    private static final class InMemoryBreaks implements BreakRepository {
        private final Map<UUID, Break> store = new HashMap<>();

        InMemoryBreaks(Break seed) {
            store.put(seed.id(), seed);
        }

        @Override
        public Break save(Break brk) {
            store.put(brk.id(), brk);
            return brk;
        }

        @Override
        public Optional<Break> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Break> findByRunId(UUID runId) {
            return store.values().stream().filter(b -> b.runId().equals(runId)).toList();
        }

        @Override
        public List<Break> findByStatus(BreakStatus status) {
            return store.values().stream().filter(b -> b.status() == status).toList();
        }
    }

    private static final class RecordingOrchestrator implements OrchestratorPaymentPort {
        final AtomicReference<String> lastAction = new AtomicReference<>();
        final AtomicReference<UUID> lastPaymentId = new AtomicReference<>();

        @Override
        public Optional<PaymentSnapshot> findById(UUID paymentId) {
            return Optional.empty();
        }

        @Override
        public PaymentSnapshot resolveReconciliation(UUID paymentId, String action, String idempotencyKey) {
            lastPaymentId.set(paymentId);
            lastAction.set(action);
            return new PaymentSnapshot(paymentId, "FAILED_FINAL", "ord");
        }
    }
}
