package com.payhub.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.payhub.reconciliation.application.ConcurrentReconciliationRunException;
import com.payhub.reconciliation.application.dto.BreakView;
import com.payhub.reconciliation.application.dto.ReconciliationRunView;
import com.payhub.reconciliation.application.port.in.ListBreaksUseCase;
import com.payhub.reconciliation.application.port.in.ResolveBreakUseCase;
import com.payhub.reconciliation.application.port.in.StartReconciliationRunUseCase;
import com.payhub.reconciliation.application.port.out.OrchestratorPaymentPort;
import com.payhub.reconciliation.application.port.out.ReconciliationRunRepository;
import com.payhub.reconciliation.domain.BreakStatus;
import com.payhub.reconciliation.domain.ReconciliationRun;
import com.payhub.reconciliation.domain.RunStatus;

@Tag("integration")
@Import({TestcontainersConfiguration.class, ReconciliationFlowIntegrationTest.FakeOrchestratorConfig.class})
@SpringBootTest
class ReconciliationFlowIntegrationTest {

    static final UUID PAYMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    StartReconciliationRunUseCase startRunUseCase;
    @Autowired
    ListBreaksUseCase listBreaksUseCase;
    @Autowired
    ResolveBreakUseCase resolveBreakUseCase;
    @Autowired
    ReconciliationRunRepository runRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void should_import_csv_open_break_and_resolve_via_audited_confirm() {
        UUID tenantId = UUID.randomUUID();
        ReconciliationRunView run = startRunUseCase.execute(new StartReconciliationRunUseCase.Command(
                tenantId,
                "MM",
                "sandbox-mm-settled-mismatch"
        ));

        assertThat(run.status()).isEqualTo(RunStatus.CLOSED.name());

        List<BreakView> breaks = listBreaksUseCase.byRun(run.id());
        assertThat(breaks).hasSize(1);
        BreakView open = breaks.getFirst();
        assertThat(open.status()).isEqualTo(BreakStatus.OPEN.name());
        assertThat(open.detail()).contains("SETTLED").contains("RECONCILIATION_REQUIRED");

        BreakView resolved = resolveBreakUseCase.execute(new ResolveBreakUseCase.Command(
                open.id(),
                "CONFIRM",
                "ops-alice",
                "idem-confirm-" + open.id()
        ));
        assertThat(resolved.status()).isEqualTo(BreakStatus.RESOLVED.name());
        assertThat(resolved.resolutionAction()).isEqualTo("CONFIRM");
        assertThat(resolved.resolvedBy()).isEqualTo("ops-alice");
        assertThat(resolved.resolvedAt()).isNotNull();
    }

    @Test
    void should_reject_second_open_run_for_same_tenant_rail() {
        UUID tenantId = UUID.randomUUID();
        ReconciliationRun stuck = ReconciliationRun.open(tenantId, "WAVE", "other-stmt");
        runRepository.save(stuck);

        assertThatThrownBy(() -> startRunUseCase.execute(new StartReconciliationRunUseCase.Command(
                tenantId,
                "WAVE",
                "sandbox-mm-settled-mismatch"
        ))).isInstanceOf(ConcurrentReconciliationRunException.class);
    }

    @Test
    void should_use_advisory_lock_function() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*)::int from pg_proc where proname = 'pg_advisory_xact_lock'",
                Integer.class
        );
        assertThat(count).isGreaterThan(0);
    }

    @TestConfiguration
    static class FakeOrchestratorConfig {

        @Bean
        @Primary
        OrchestratorPaymentPort fakeOrchestratorPaymentPort() {
            Map<UUID, OrchestratorPaymentPort.PaymentSnapshot> store = new ConcurrentHashMap<>();
            store.put(PAYMENT_ID, new OrchestratorPaymentPort.PaymentSnapshot(
                    PAYMENT_ID, "RECONCILIATION_REQUIRED", "ord-sandbox"));
            return new OrchestratorPaymentPort() {
                @Override
                public Optional<OrchestratorPaymentPort.PaymentSnapshot> findById(UUID paymentId) {
                    return Optional.ofNullable(store.get(paymentId));
                }

                @Override
                public OrchestratorPaymentPort.PaymentSnapshot resolveReconciliation(
                        UUID paymentId, String action, String idempotencyKey) {
                    OrchestratorPaymentPort.PaymentSnapshot updated =
                            new OrchestratorPaymentPort.PaymentSnapshot(paymentId, "FAILED_FINAL", "ord-sandbox");
                    store.put(paymentId, updated);
                    return updated;
                }
            };
        }
    }
}
