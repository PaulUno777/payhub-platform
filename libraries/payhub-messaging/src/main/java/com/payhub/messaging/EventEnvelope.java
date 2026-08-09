package com.payhub.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Catalog-minimum event envelope (plan §6). Technical contract only — no domain types.
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        String schemaVersion,
        Instant occurredAt,
        String producer,
        UUID aggregateId,
        UUID tenantId,
        String traceparent,
        String causationId,
        String payloadJson
) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(producer, "producer");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(payloadJson, "payloadJson");
    }
}
