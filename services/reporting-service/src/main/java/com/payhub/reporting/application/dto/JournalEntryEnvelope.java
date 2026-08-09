package com.payhub.reporting.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka envelope for ledger.journal-entry.v1.
 * Payload fields mirror FinLedger TransactionPosted.
 */
public record JournalEntryEnvelope(
        UUID eventId,
        String eventType,
        String schemaVersion,
        Instant occurredAt,
        String producer,
        UUID aggregateId,
        UUID tenantId,
        String traceparent,
        String causationId,
        TransactionPostedPayload payload
) {

    public record TransactionPostedPayload(
            UUID tenantId,
            UUID journalEntryId,
            String transactionReference,
            String type,
            List<PostingSummary> postings,
            Instant occurredAt
    ) {
    }

    public record PostingSummary(
            UUID accountId,
            String amount,
            String currencyCode,
            String settlementStatus
    ) {
    }
}
