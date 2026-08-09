package com.payhub.reconciliation.application.usecase;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.reconciliation.application.ConcurrentReconciliationRunException;
import com.payhub.reconciliation.application.dto.ReconciliationRunView;
import com.payhub.reconciliation.application.port.in.StartReconciliationRunUseCase;
import com.payhub.reconciliation.application.port.out.BreakRepository;
import com.payhub.reconciliation.application.port.out.OrchestratorPaymentPort;
import com.payhub.reconciliation.application.port.out.OrchestratorPaymentPort.PaymentSnapshot;
import com.payhub.reconciliation.application.port.out.ReconciliationRunRepository;
import com.payhub.reconciliation.application.port.out.RunLockPort;
import com.payhub.reconciliation.application.port.out.StatementImportStore;
import com.payhub.reconciliation.application.port.out.StatementSourcePort;
import com.payhub.reconciliation.application.port.out.StatementSourcePort.StatementLine;
import com.payhub.reconciliation.domain.Break;
import com.payhub.reconciliation.domain.BreakType;
import com.payhub.reconciliation.domain.ReconciliationRun;

@Service
public class StartReconciliationRunService implements StartReconciliationRunUseCase {

    private final RunLockPort runLockPort;
    private final ReconciliationRunRepository runRepository;
    private final BreakRepository breakRepository;
    private final StatementSourcePort statementSourcePort;
    private final StatementImportStore statementImportStore;
    private final OrchestratorPaymentPort orchestratorPaymentPort;

    public StartReconciliationRunService(
            RunLockPort runLockPort,
            ReconciliationRunRepository runRepository,
            BreakRepository breakRepository,
            StatementSourcePort statementSourcePort,
            StatementImportStore statementImportStore,
            OrchestratorPaymentPort orchestratorPaymentPort
    ) {
        this.runLockPort = runLockPort;
        this.runRepository = runRepository;
        this.breakRepository = breakRepository;
        this.statementSourcePort = statementSourcePort;
        this.statementImportStore = statementImportStore;
        this.orchestratorPaymentPort = orchestratorPaymentPort;
    }

    @Override
    @Transactional
    public ReconciliationRunView execute(Command command) {
        Objects.requireNonNull(command.tenantId(), "tenantId");
        Objects.requireNonNull(command.railCode(), "railCode");
        Objects.requireNonNull(command.statementKey(), "statementKey");

        runLockPort.lock(command.tenantId(), command.railCode());

        if (runRepository.findOpen(command.tenantId(), command.railCode()).isPresent()) {
            throw new ConcurrentReconciliationRunException(
                    "Open reconciliation run already exists for tenant/rail");
        }

        ReconciliationRun run = ReconciliationRun.open(
                command.tenantId(),
                command.railCode(),
                command.statementKey()
        );
        runRepository.save(run);

        var lines = statementSourcePort.load(command.statementKey());
        if (!statementImportStore.alreadyImported(command.statementKey())) {
            statementImportStore.markImported(command.statementKey());
        }

        for (StatementLine line : lines) {
            Optional<PaymentSnapshot> payment = orchestratorPaymentPort.findById(line.paymentId());
            correlate(run, line, payment).ifPresent(breakRepository::save);
        }

        run.close();
        runRepository.save(run);
        return ReconciliationRunView.from(run);
    }

    static Optional<Break> correlate(ReconciliationRun run, StatementLine line, Optional<PaymentSnapshot> payment) {
        String statementStatus = line.statementStatus() == null ? "" : line.statementStatus().trim().toUpperCase();
        if (payment.isEmpty()) {
            return Optional.of(Break.open(
                    run.id(),
                    line.paymentId(),
                    line.externalRef(),
                    BreakType.STATUS_MISMATCH,
                    "statement " + statementStatus + " vs payment MISSING"
            ));
        }
        String paymentStatus = payment.get().status();
        if ("SETTLED".equals(statementStatus)
                && ("RECONCILIATION_REQUIRED".equals(paymentStatus)
                || "SETTLEMENT_PENDING".equals(paymentStatus))) {
            return Optional.of(Break.open(
                    run.id(),
                    line.paymentId(),
                    line.externalRef(),
                    BreakType.STATUS_MISMATCH,
                    "statement SETTLED vs payment " + paymentStatus
            ));
        }
        if ("FAILED".equals(statementStatus)
                && ("SETTLEMENT_PENDING".equals(paymentStatus)
                || "RECONCILIATION_REQUIRED".equals(paymentStatus)
                || "RAIL_SUBMITTED".equals(paymentStatus))) {
            return Optional.of(Break.open(
                    run.id(),
                    line.paymentId(),
                    line.externalRef(),
                    BreakType.STATUS_MISMATCH,
                    "statement FAILED vs payment " + paymentStatus
            ));
        }
        return Optional.empty();
    }
}
