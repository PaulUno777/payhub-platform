package com.payhub.orchestrator.infrastructure.temporal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.temporal")
public record TemporalProperties(
        boolean enabled,
        String target,
        String namespace,
        String taskQueue
) {

    public TemporalProperties {
        if (target == null || target.isBlank()) {
            target = "localhost:7233";
        }
        if (namespace == null || namespace.isBlank()) {
            namespace = "default";
        }
        if (taskQueue == null || taskQueue.isBlank()) {
            taskQueue = "payment-capture";
        }
    }
}
