package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

public interface RiskPort {

    RiskDecision evaluate(RiskCommand command);

    enum RiskDecision {
        APPROVED,
        REJECTED,
        REVIEW
    }

    record RiskCommand(
            UUID paymentId,
            UUID merchantId,
            UUID tenantId,
            String amount,
            String currencyCode
    ) {
    }
}
