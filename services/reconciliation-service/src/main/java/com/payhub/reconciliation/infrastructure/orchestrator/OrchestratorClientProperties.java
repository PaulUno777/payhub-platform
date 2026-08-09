package com.payhub.reconciliation.infrastructure.orchestrator;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.clients")
public record OrchestratorClientProperties(String paymentOrchestrator) {

    public OrchestratorClientProperties {
        if (paymentOrchestrator == null || paymentOrchestrator.isBlank()) {
            paymentOrchestrator = "http://localhost:8200";
        }
    }
}
