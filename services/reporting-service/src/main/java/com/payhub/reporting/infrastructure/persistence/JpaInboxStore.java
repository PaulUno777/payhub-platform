package com.payhub.reporting.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.reporting.application.port.out.InboxStore;

@Component
public class JpaInboxStore implements InboxStore {

    private final InboxEventJpaRepository repository;

    public JpaInboxStore(InboxEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public void save(UUID eventId, String consumerName) {
        repository.save(new InboxEventEntity(eventId, consumerName, Instant.now()));
    }
}
