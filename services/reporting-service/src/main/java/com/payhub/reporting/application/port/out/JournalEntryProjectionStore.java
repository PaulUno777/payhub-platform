package com.payhub.reporting.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryProjectionStore {

    void upsert(JournalEntryProjection projection);

    Optional<JournalEntryProjection> findByJournalEntryId(UUID journalEntryId);

    long count();

    record JournalEntryProjection(
            UUID journalEntryId,
            UUID tenantId,
            UUID eventId,
            String transactionReference,
            String type,
            Instant occurredAt,
            Instant asOf
    ) {
    }
}
