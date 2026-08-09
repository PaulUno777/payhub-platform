package com.payhub.reconciliation.application.usecase;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.reconciliation.application.BreakNotFoundException;
import com.payhub.reconciliation.application.IllegalReconciliationTransitionException;
import com.payhub.reconciliation.application.dto.BreakView;
import com.payhub.reconciliation.application.port.in.ResolveBreakUseCase;
import com.payhub.reconciliation.application.port.out.BreakRepository;
import com.payhub.reconciliation.application.port.out.OrchestratorPaymentPort;
import com.payhub.reconciliation.domain.Break;
import com.payhub.reconciliation.domain.BreakStatus;
import com.payhub.reconciliation.domain.IllegalReconciliationStateException;
import com.payhub.reconciliation.domain.ResolveAction;

@Service
public class ResolveBreakService implements ResolveBreakUseCase {

    private final BreakRepository breakRepository;
    private final OrchestratorPaymentPort orchestratorPaymentPort;

    public ResolveBreakService(
            BreakRepository breakRepository,
            OrchestratorPaymentPort orchestratorPaymentPort
    ) {
        this.breakRepository = breakRepository;
        this.orchestratorPaymentPort = orchestratorPaymentPort;
    }

    @Override
    @Transactional
    public BreakView execute(Command command) {
        Objects.requireNonNull(command.breakId(), "breakId");
        Objects.requireNonNull(command.action(), "action");
        Objects.requireNonNull(command.actor(), "actor");
        Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey");

        ResolveAction action;
        try {
            action = ResolveAction.valueOf(command.action().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalReconciliationTransitionException("Unknown resolve action: " + command.action(), ex);
        }

        Break brk = breakRepository.findById(command.breakId())
                .orElseThrow(() -> new BreakNotFoundException(command.breakId()));

        if (brk.status() == BreakStatus.RESOLVED) {
            return BreakView.from(brk);
        }

        if (action == ResolveAction.REQUEST_REVERSAL) {
            orchestratorPaymentPort.resolveReconciliation(
                    brk.paymentId(),
                    ResolveAction.REQUEST_REVERSAL.name(),
                    command.idempotencyKey()
            );
        }

        try {
            brk.resolve(action, command.actor());
        } catch (IllegalReconciliationStateException ex) {
            throw new IllegalReconciliationTransitionException(ex.getMessage(), ex);
        }
        return BreakView.from(breakRepository.save(brk));
    }
}
