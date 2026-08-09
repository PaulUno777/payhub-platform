package com.payhub.messaging.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Transactional outbox writer / reader. Publish happens outside the business TX via relay.
 */
public interface OutboxWriter {

    void append(OutboxAppend append);

    List<OutboxRecord> findUnpublished(int limit);

    void markPublished(UUID id, Instant publishedAt);

    record OutboxAppend(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String payloadJson,
            UUID tenantId,
            Instant occurredAt
    ) {
        public OutboxAppend {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(aggregateType, "aggregateType");
            Objects.requireNonNull(aggregateId, "aggregateId");
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(topic, "topic");
            Objects.requireNonNull(payloadJson, "payloadJson");
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(occurredAt, "occurredAt");
        }
    }

    record OutboxRecord(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String payloadJson,
            UUID tenantId,
            Instant occurredAt,
            Instant createdAt,
            Optional<Instant> publishedAt
    ) {
    }
}
