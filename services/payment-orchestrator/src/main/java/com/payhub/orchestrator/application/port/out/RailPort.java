package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

/**
 * Rail submission port — MmSandbox via rail-adapter (DS-008).
 */
public interface RailPort {

    RailSubmitResult submit(RailSubmitCommand command);

    RailProofResult awaitFinalProof(RailProofCommand command);

    enum RailResult {
        ACCEPTED,
        REJECTED,
        AMBIGUOUS
    }

    record RailSubmitCommand(
            UUID paymentId,
            String amount,
            String currencyCode,
            String sandboxMode
    ) {
    }

    record RailSubmitResult(RailResult outcome, String providerReference) {
    }

    record RailProofCommand(UUID paymentId, String providerReference, String sandboxMode) {
    }

    record RailProofResult(RailResult outcome) {
    }
}
