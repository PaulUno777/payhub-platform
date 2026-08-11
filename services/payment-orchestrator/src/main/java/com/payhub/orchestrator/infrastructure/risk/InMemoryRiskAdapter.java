package com.payhub.orchestrator.infrastructure.risk;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.RiskPort;

@Component
@ConditionalOnProperty(prefix = "payhub.risk", name = "mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryRiskAdapter implements RiskPort {

    private final AtomicReference<RiskDecision> decision;

    public InMemoryRiskAdapter(
            @org.springframework.beans.factory.annotation.Value("${payhub.risk.in-memory-default-decision:APPROVED}")
            String defaultDecision
    ) {
        this.decision = new AtomicReference<>(RiskDecision.valueOf(defaultDecision.trim().toUpperCase()));
    }

    public void setDecision(RiskDecision decision) {
        this.decision.set(decision);
    }

    @Override
    public RiskDecision evaluate(RiskCommand command) {
        return decision.get();
    }
}
