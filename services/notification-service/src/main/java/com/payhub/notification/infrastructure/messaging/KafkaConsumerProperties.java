package com.payhub.notification.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.kafka")
public record KafkaConsumerProperties(
        String bootstrapServers,
        String paymentLifecycleTopic,
        String paymentLifecycleConsumerGroup,
        Integer concurrency
) {

    public KafkaConsumerProperties {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            bootstrapServers = "localhost:9092";
        }
        if (paymentLifecycleTopic == null || paymentLifecycleTopic.isBlank()) {
            paymentLifecycleTopic = "payment.lifecycle.v1";
        }
        if (paymentLifecycleConsumerGroup == null || paymentLifecycleConsumerGroup.isBlank()) {
            paymentLifecycleConsumerGroup = "notification-payment-lifecycle-v1";
        }
        if (concurrency == null || concurrency < 1) {
            concurrency = 2;
        }
    }
}
