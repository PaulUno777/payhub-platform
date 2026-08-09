package com.payhub.railadapter.infrastructure.mmsandbox;


import org.springframework.stereotype.Component;

import com.payhub.railadapter.application.port.out.RailProviderPort;
import com.payhub.railadapter.domain.RailOutcome;
import com.payhub.railadapter.domain.SandboxMode;

/**
 * In-process mobile-money sandbox — deterministic outcomes for DS-008.
 */
@Component
public class MmSandboxProvider implements RailProviderPort {

    @Override
    public SubmitResult submit(SubmitCommand command) {
        SandboxMode mode = command.mode() == null ? SandboxMode.ACCEPT : command.mode();
        return switch (mode) {
            case REJECT -> new SubmitResult(RailOutcome.REJECTED, null);
            case AMBIGUOUS -> new SubmitResult(RailOutcome.AMBIGUOUS, null);
            case ACCEPT, PROOF_AMBIGUOUS -> new SubmitResult(
                    RailOutcome.ACCEPTED,
                    "mm-" + command.paymentId()
            );
        };
    }

    @Override
    public ProofResult awaitProof(ProofCommand command) {
        SandboxMode mode = command.mode() == null ? SandboxMode.ACCEPT : command.mode();
        if (mode == SandboxMode.PROOF_AMBIGUOUS) {
            return new ProofResult(RailOutcome.AMBIGUOUS);
        }
        if (command.providerReference() == null || command.providerReference().isBlank()) {
            return new ProofResult(RailOutcome.AMBIGUOUS);
        }
        return new ProofResult(RailOutcome.ACCEPTED);
    }
}
