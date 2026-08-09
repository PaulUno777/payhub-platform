package com.payhub.reporting.adapter.in.event;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.payhub.reporting.application.dto.JournalEntryEnvelope;
import com.payhub.reporting.application.port.in.ApplyJournalEntryProjectionUseCase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Transport adapter: maps Kafka (PayHub envelope or Debezium EventRouter payload) to use case.
 */
@Component
@ConditionalOnProperty(prefix = "payhub.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JournalEntryKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(JournalEntryKafkaListener.class);

    private final ApplyJournalEntryProjectionUseCase applyJournalEntryProjectionUseCase;
    private final ObjectMapper objectMapper;

    public JournalEntryKafkaListener(
            ApplyJournalEntryProjectionUseCase applyJournalEntryProjectionUseCase,
            ObjectMapper objectMapper
    ) {
        this.applyJournalEntryProjectionUseCase = applyJournalEntryProjectionUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${payhub.kafka.journal-entry-topic:ledger.journal-entry.v1}",
            groupId = "${payhub.kafka.consumer-group:reporting-ledger-journal-v1}"
    )
    public void onMessage(ConsumerRecord<String, String> record) throws IOException {
        JournalEntryEnvelope envelope = parse(record);
        boolean applied = applyJournalEntryProjectionUseCase.execute(envelope);
        if (!applied) {
            log.debug("Skipped duplicate journal entry eventId={}", envelope.eventId());
        }
    }

    JournalEntryEnvelope parse(ConsumerRecord<String, String> record) throws IOException {
        JsonNode root = objectMapper.readTree(record.value());
        if (root.hasNonNull("eventId") && root.has("payload")) {
            return objectMapper.treeToValue(root, JournalEntryEnvelope.class);
        }

        // Debezium Outbox EventRouter: value = TransactionPosted JSON; id in header
        UUID eventId = headerUuid(record, "id");
        if (eventId == null) {
            throw new IllegalArgumentException("Missing eventId in body and id header");
        }
        JournalEntryEnvelope.TransactionPostedPayload payload =
                objectMapper.treeToValue(root, JournalEntryEnvelope.TransactionPostedPayload.class);
        UUID tenantId = headerUuid(record, "tenantId");
        if (tenantId == null) {
            tenantId = payload.tenantId();
        }
        return new JournalEntryEnvelope(
                eventId,
                "TransactionPosted",
                "1",
                payload.occurredAt() != null ? payload.occurredAt() : Instant.now(),
                "finledger",
                payload.journalEntryId(),
                tenantId,
                null,
                null,
                payload
        );
    }

    private static UUID headerUuid(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null) {
            return null;
        }
        String raw = new String(header.value(), StandardCharsets.UTF_8).replace("\"", "");
        return UUID.fromString(raw);
    }
}
