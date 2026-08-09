package com.payhub.reporting.application.usecase;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.reporting.application.dto.JournalEntryEnvelope;
import com.payhub.reporting.application.port.in.ApplyJournalEntryProjectionUseCase;
import com.payhub.reporting.application.port.out.InboxStore;
import com.payhub.reporting.application.port.out.JournalEntryProjectionStore;
import com.payhub.reporting.application.port.out.JournalEntryProjectionStore.JournalEntryProjection;

@Service
public class ApplyJournalEntryProjectionService implements ApplyJournalEntryProjectionUseCase {

    public static final String CONSUMER = "reporting-ledger-journal-v1";

    private final InboxStore inboxStore;
    private final JournalEntryProjectionStore projectionStore;

    public ApplyJournalEntryProjectionService(
            InboxStore inboxStore,
            JournalEntryProjectionStore projectionStore
    ) {
        this.inboxStore = inboxStore;
        this.projectionStore = projectionStore;
    }

    @Override
    @Transactional
    public boolean execute(JournalEntryEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(envelope.eventId(), "eventId");
        Objects.requireNonNull(envelope.payload(), "payload");

        if (inboxStore.exists(envelope.eventId())) {
            return false;
        }

        var payload = envelope.payload();
        Instant asOf = Instant.now();
        projectionStore.upsert(new JournalEntryProjection(
                payload.journalEntryId(),
                payload.tenantId(),
                envelope.eventId(),
                payload.transactionReference(),
                payload.type(),
                payload.occurredAt(),
                asOf
        ));
        inboxStore.save(envelope.eventId(), CONSUMER);
        return true;
    }
}
