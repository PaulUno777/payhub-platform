package com.payhub.merchant.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.payhub.merchant.application.port.out.IdempotencyStore;

@Component
public class JpaIdempotencyStore implements IdempotencyStore {

    private final IdempotencyJpaRepository repository;

    public JpaIdempotencyStore(IdempotencyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UUID> findMerchantId(String operation, String idempotencyKey) {
        return repository.findById(new IdempotencyId(operation, idempotencyKey))
                .map(entity -> entity.getMerchantId());
    }

    @Override
    public void save(String operation, String idempotencyKey, String requestHash, UUID merchantId) {
        try {
            repository.save(new IdempotencyJpaEntity(
                    operation,
                    idempotencyKey,
                    requestHash,
                    merchantId,
                    Instant.now()
            ));
        } catch (DataIntegrityViolationException ex) {
            throw ex;
        }
    }
}
