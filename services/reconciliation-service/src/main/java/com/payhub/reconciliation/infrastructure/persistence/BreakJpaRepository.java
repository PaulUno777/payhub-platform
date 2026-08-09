package com.payhub.reconciliation.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface BreakJpaRepository extends JpaRepository<BreakJpaEntity, UUID> {

    List<BreakJpaEntity> findByRunId(UUID runId);

    List<BreakJpaEntity> findByStatus(String status);
}
