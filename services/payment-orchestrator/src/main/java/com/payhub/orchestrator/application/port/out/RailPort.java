package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

/**
 * Rail submission port — stubbed in DS-006; real MmSandbox path is DS-008.
 */
public interface RailPort {

    RailResult submit(RailSubmitCommand command);

    enum RailResult {
        ACCEPTED,
        REJECTED,
        AMBIGUOUS
    }

    record RailSubmitCommand(UUID paymentId, String amount, String currencyCode) {
    }
}
