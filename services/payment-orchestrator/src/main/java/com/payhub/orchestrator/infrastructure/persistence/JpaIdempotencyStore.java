package com.payhub.orchestrator.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.IdempotencyStore;

@Component
public class JpaIdempotencyStore implements IdempotencyStore {

    private final IdempotencyJpaRepository repository;

    public JpaIdempotencyStore(IdempotencyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UUID> findPaymentId(String operation, String idempotencyKey) {
        return repository.findById(new IdempotencyId(operation, idempotencyKey))
                .map(entity -> entity.getPaymentId());
    }

    @Override
    public void save(String operation, String idempotencyKey, String requestHash, UUID paymentId) {
        repository.save(new IdempotencyJpaEntity(
                operation, idempotencyKey, requestHash, paymentId, Instant.now()));
    }
}
