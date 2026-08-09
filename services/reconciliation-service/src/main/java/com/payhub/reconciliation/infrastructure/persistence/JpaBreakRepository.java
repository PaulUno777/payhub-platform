package com.payhub.reconciliation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.reconciliation.application.port.out.BreakRepository;
import com.payhub.reconciliation.domain.Break;
import com.payhub.reconciliation.domain.BreakStatus;

@Component
public class JpaBreakRepository implements BreakRepository {

    private final BreakJpaRepository jpaRepository;

    public JpaBreakRepository(BreakJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Break save(Break brk) {
        return ReconciliationMapper.toBreak(jpaRepository.save(ReconciliationMapper.toEntity(brk)));
    }

    @Override
    public Optional<Break> findById(UUID id) {
        return jpaRepository.findById(id).map(ReconciliationMapper::toBreak);
    }

    @Override
    public List<Break> findByRunId(UUID runId) {
        return jpaRepository.findByRunId(runId).stream().map(ReconciliationMapper::toBreak).toList();
    }

    @Override
    public List<Break> findByStatus(BreakStatus status) {
        return jpaRepository.findByStatus(status.name()).stream().map(ReconciliationMapper::toBreak).toList();
    }
}
