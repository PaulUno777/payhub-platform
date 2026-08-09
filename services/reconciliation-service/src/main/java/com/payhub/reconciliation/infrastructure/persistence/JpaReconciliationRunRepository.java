package com.payhub.reconciliation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.reconciliation.application.port.out.ReconciliationRunRepository;
import com.payhub.reconciliation.domain.ReconciliationRun;
import com.payhub.reconciliation.domain.RunStatus;

@Component
public class JpaReconciliationRunRepository implements ReconciliationRunRepository {

    private final ReconciliationRunJpaRepository jpaRepository;

    public JpaReconciliationRunRepository(ReconciliationRunJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ReconciliationRun save(ReconciliationRun run) {
        return ReconciliationMapper.toRun(jpaRepository.save(ReconciliationMapper.toEntity(run)));
    }

    @Override
    public Optional<ReconciliationRun> findById(UUID id) {
        return jpaRepository.findById(id).map(ReconciliationMapper::toRun);
    }

    @Override
    public Optional<ReconciliationRun> findOpen(UUID tenantId, String railCode) {
        return jpaRepository.findOpen(tenantId, railCode).map(ReconciliationMapper::toRun);
    }

    @Override
    public List<ReconciliationRun> findByStatus(RunStatus status) {
        return jpaRepository.findByStatus(status.name()).stream().map(ReconciliationMapper::toRun).toList();
    }
}
