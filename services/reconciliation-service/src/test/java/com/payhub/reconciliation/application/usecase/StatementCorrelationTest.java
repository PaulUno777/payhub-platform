package com.payhub.reconciliation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.payhub.reconciliation.application.port.out.OrchestratorPaymentPort.PaymentSnapshot;
import com.payhub.reconciliation.application.port.out.StatementSourcePort.StatementLine;
import com.payhub.reconciliation.domain.Break;
import com.payhub.reconciliation.domain.BreakType;
import com.payhub.reconciliation.domain.ReconciliationRun;

@Tag("unit")
class StatementCorrelationTest {

    @Test
    void should_open_break_when_statement_settled_and_payment_reconciliation_required() {
        ReconciliationRun run = ReconciliationRun.open(UUID.randomUUID(), "MM", "stmt-1");
        UUID paymentId = UUID.randomUUID();
        StatementLine line = new StatementLine("ext-1", paymentId, "SETTLED", "10.00", "XOF");
        PaymentSnapshot payment = new PaymentSnapshot(paymentId, "RECONCILIATION_REQUIRED", "ord-1");

        Optional<Break> brk = StartReconciliationRunService.correlate(run, line, Optional.of(payment));

        assertThat(brk).isPresent();
        assertThat(brk.get().type()).isEqualTo(BreakType.STATUS_MISMATCH);
        assertThat(brk.get().detail()).contains("SETTLED").contains("RECONCILIATION_REQUIRED");
    }

    @Test
    void should_open_break_when_statement_failed_and_payment_settlement_pending() {
        ReconciliationRun run = ReconciliationRun.open(UUID.randomUUID(), "MM", "stmt-1");
        UUID paymentId = UUID.randomUUID();
        StatementLine line = new StatementLine("ext-2", paymentId, "FAILED", "10.00", "XOF");
        PaymentSnapshot payment = new PaymentSnapshot(paymentId, "SETTLEMENT_PENDING", "ord-2");

        Optional<Break> brk = StartReconciliationRunService.correlate(run, line, Optional.of(payment));

        assertThat(brk).isPresent();
    }

    @Test
    void should_not_open_break_when_statuses_align() {
        ReconciliationRun run = ReconciliationRun.open(UUID.randomUUID(), "MM", "stmt-1");
        UUID paymentId = UUID.randomUUID();
        StatementLine line = new StatementLine("ext-3", paymentId, "SETTLED", "10.00", "XOF");
        PaymentSnapshot payment = new PaymentSnapshot(paymentId, "SETTLED", "ord-3");

        assertThat(StartReconciliationRunService.correlate(run, line, Optional.of(payment))).isEmpty();
    }
}
