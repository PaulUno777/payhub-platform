package com.payhub.orchestrator.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.outbox.relay")
public record OutboxRelayProperties(
        boolean enabled,
        int batchSize,
        long pollIntervalMs
) {

    public OutboxRelayProperties {
        if (batchSize <= 0) {
            batchSize = 50;
        }
        if (pollIntervalMs <= 0) {
            pollIntervalMs = 1000L;
        }
    }
}
