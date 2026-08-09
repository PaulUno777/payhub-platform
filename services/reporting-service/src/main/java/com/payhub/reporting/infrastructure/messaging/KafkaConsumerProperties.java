package com.payhub.reporting.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.kafka")
public record KafkaConsumerProperties(
        String bootstrapServers,
        String journalEntryTopic,
        String consumerGroup
) {

    public KafkaConsumerProperties {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            bootstrapServers = "localhost:9092";
        }
        if (journalEntryTopic == null || journalEntryTopic.isBlank()) {
            journalEntryTopic = "ledger.journal-entry.v1";
        }
        if (consumerGroup == null || consumerGroup.isBlank()) {
            consumerGroup = "reporting-ledger-journal-v1";
        }
    }
}
