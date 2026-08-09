package com.payhub.railadapter.application.port.out;

import java.util.UUID;

import com.payhub.railadapter.domain.RailOutcome;
import com.payhub.railadapter.domain.SandboxMode;

/**
 * PSP-only port — MmSandbox; never FinLedger.
 */
public interface RailProviderPort {

    SubmitResult submit(SubmitCommand command);

    ProofResult awaitProof(ProofCommand command);

    SubmitResult submitRefund(SubmitCommand command);

    ProofResult awaitRefundProof(ProofCommand command);

    record SubmitCommand(UUID paymentId, String amount, String currencyCode, SandboxMode mode) {
    }

    record SubmitResult(RailOutcome outcome, String providerReference) {
    }

    record ProofCommand(UUID paymentId, String providerReference, SandboxMode mode) {
    }

    record ProofResult(RailOutcome outcome) {
    }
}
