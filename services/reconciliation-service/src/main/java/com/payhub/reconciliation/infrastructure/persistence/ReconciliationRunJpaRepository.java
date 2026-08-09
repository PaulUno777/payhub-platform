package com.payhub.reconciliation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReconciliationRunJpaRepository extends JpaRepository<ReconciliationRunJpaEntity, UUID> {

    @Query("select r from ReconciliationRunJpaEntity r where r.tenantId = :tenantId and r.railCode = :railCode and r.status = 'OPEN'")
    Optional<ReconciliationRunJpaEntity> findOpen(
            @Param("tenantId") UUID tenantId,
            @Param("railCode") String railCode
    );

    List<ReconciliationRunJpaEntity> findByStatus(String status);
}
