package com.payhub.orchestrator.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.messaging.outbox.OutboxWriter;

@Component
public class JpaOutboxWriter implements OutboxWriter {

    private final OutboxEventJpaRepository repository;

    public JpaOutboxWriter(OutboxEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void append(OutboxAppend append) {
        repository.save(new OutboxEventJpaEntity(
                append.id(),
                append.aggregateType(),
                append.aggregateId(),
                append.eventType(),
                append.topic(),
                append.payloadJson(),
                append.tenantId(),
                append.occurredAt(),
                Instant.now()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxRecord> findUnpublished(int limit) {
        return repository.findUnpublished(limit).stream()
                .map(e -> new OutboxRecord(
                        e.getId(),
                        e.getAggregateType(),
                        e.getAggregateId(),
                        e.getEventType(),
                        e.getTopic(),
                        e.getPayload(),
                        e.getTenantId(),
                        e.getOccurredAt(),
                        e.getCreatedAt(),
                        Optional.ofNullable(e.getPublishedAt())
                ))
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(UUID id, Instant publishedAt) {
        OutboxEventJpaEntity entity = repository.findById(id).orElseThrow();
        entity.setPublishedAt(publishedAt);
        repository.save(entity);
    }
}
