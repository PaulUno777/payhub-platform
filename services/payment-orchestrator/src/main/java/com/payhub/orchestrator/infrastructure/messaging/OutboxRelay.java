package com.payhub.orchestrator.infrastructure.messaging;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.payhub.messaging.outbox.OutboxWriter;
import com.payhub.messaging.outbox.OutboxWriter.OutboxRecord;

@Component
@ConditionalOnProperty(prefix = "payhub.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(KafkaTemplate.class)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxWriter outboxWriter;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRelayProperties properties;

    public OutboxRelay(
            OutboxWriter outboxWriter,
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxRelayProperties properties
    ) {
        this.outboxWriter = outboxWriter;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${payhub.outbox.relay.poll-interval-ms:1000}")
    public void publishUnpublished() {
        List<OutboxRecord> batch = outboxWriter.findUnpublished(properties.batchSize());
        for (OutboxRecord record : batch) {
            try {
                kafkaTemplate.send(record.topic(), record.aggregateId().toString(), record.payloadJson()).get();
                outboxWriter.markPublished(record.id(), Instant.now());
            } catch (Exception ex) {
                log.warn("Outbox relay failed for id={}: {}", record.id(), ex.toString());
                return;
            }
        }
    }
}
