package com.payhub.reporting.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.reporting.application.port.out.JournalEntryProjectionStore;

@Component
public class JpaJournalEntryProjectionStore implements JournalEntryProjectionStore {

    private final JournalEntryProjectionJpaRepository repository;

    public JpaJournalEntryProjectionStore(JournalEntryProjectionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void upsert(JournalEntryProjection projection) {
        JournalEntryProjectionEntity entity = new JournalEntryProjectionEntity();
        entity.setJournalEntryId(projection.journalEntryId());
        entity.setTenantId(projection.tenantId());
        entity.setEventId(projection.eventId());
        entity.setTransactionReference(projection.transactionReference());
        entity.setEntryType(projection.type());
        entity.setOccurredAt(projection.occurredAt());
        entity.setAsOf(projection.asOf());
        repository.save(entity);
    }

    @Override
    public Optional<JournalEntryProjection> findByJournalEntryId(UUID journalEntryId) {
        return repository.findById(journalEntryId).map(this::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }

    private JournalEntryProjection toDomain(JournalEntryProjectionEntity entity) {
        return new JournalEntryProjection(
                entity.getJournalEntryId(),
                entity.getTenantId(),
                entity.getEventId(),
                entity.getTransactionReference(),
                entity.getEntryType(),
                entity.getOccurredAt(),
                entity.getAsOf()
        );
    }
}
