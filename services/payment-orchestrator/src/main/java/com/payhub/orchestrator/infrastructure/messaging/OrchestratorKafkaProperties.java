package com.payhub.orchestrator.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.kafka")
public record OrchestratorKafkaProperties(
        boolean enabled,
        String bootstrapServers,
        String paymentLifecycleTopic
) {

    public OrchestratorKafkaProperties {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            bootstrapServers = "localhost:9092";
        }
        if (paymentLifecycleTopic == null || paymentLifecycleTopic.isBlank()) {
            paymentLifecycleTopic = "payment.lifecycle.v1";
        }
    }
}
