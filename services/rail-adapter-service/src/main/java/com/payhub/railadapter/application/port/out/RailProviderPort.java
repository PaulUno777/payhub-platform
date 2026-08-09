package com.payhub.railadapter.application.port.out;

import java.util.UUID;

import com.payhub.railadapter.domain.RailOutcome;
import com.payhub.railadapter.domain.SandboxMode;

/**
 * PSP-only port — MmSandbox in DS-008; never FinLedger.
 */
public interface RailProviderPort {

    SubmitResult submit(SubmitCommand command);

    ProofResult awaitProof(ProofCommand command);

    record SubmitCommand(UUID paymentId, String amount, String currencyCode, SandboxMode mode) {
    }

    record SubmitResult(RailOutcome outcome, String providerReference) {
    }

    record ProofCommand(UUID paymentId, String providerReference, SandboxMode mode) {
    }

    record ProofResult(RailOutcome outcome) {
    }
}
