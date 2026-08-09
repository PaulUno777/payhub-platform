package com.payhub.orchestrator.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyJpaEntity, IdempotencyId> {
}
