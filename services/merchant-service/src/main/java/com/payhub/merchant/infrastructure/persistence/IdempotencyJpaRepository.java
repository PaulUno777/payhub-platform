package com.payhub.merchant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyJpaEntity, IdempotencyId> {
}
