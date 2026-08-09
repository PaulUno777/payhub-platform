package com.payhub.reporting.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "journal_entry_projection")
public class JournalEntryProjectionEntity {

    @Id
    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "transaction_reference", nullable = false)
    private String transactionReference;

    @Column(name = "entry_type", nullable = false, length = 64)
    private String entryType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "as_of", nullable = false)
    private Instant asOf;

    protected JournalEntryProjectionEntity() {
    }

    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Instant getAsOf() { return asOf; }
    public void setAsOf(Instant asOf) { this.asOf = asOf; }
}
