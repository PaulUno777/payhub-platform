package com.payhub.reconciliation.infrastructure.persistence;

import com.payhub.reconciliation.domain.Break;
import com.payhub.reconciliation.domain.BreakStatus;
import com.payhub.reconciliation.domain.BreakType;
import com.payhub.reconciliation.domain.ReconciliationRun;
import com.payhub.reconciliation.domain.ResolveAction;
import com.payhub.reconciliation.domain.RunStatus;

final class ReconciliationMapper {

    private ReconciliationMapper() {
    }

    static ReconciliationRunJpaEntity toEntity(ReconciliationRun run) {
        ReconciliationRunJpaEntity entity = new ReconciliationRunJpaEntity();
        entity.setId(run.id());
        entity.setTenantId(run.tenantId());
        entity.setRailCode(run.railCode());
        entity.setStatementKey(run.statementKey());
        entity.setStatus(run.status().name());
        entity.setCreatedAt(run.createdAt());
        entity.setUpdatedAt(run.updatedAt());
        return entity;
    }

    static ReconciliationRun toRun(ReconciliationRunJpaEntity entity) {
        return ReconciliationRun.rehydrate(
                entity.getId(),
                entity.getTenantId(),
                entity.getRailCode(),
                entity.getStatementKey(),
                RunStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static BreakJpaEntity toEntity(Break brk) {
        BreakJpaEntity entity = new BreakJpaEntity();
        entity.setId(brk.id());
        entity.setRunId(brk.runId());
        entity.setPaymentId(brk.paymentId());
        entity.setExternalRef(brk.externalRef());
        entity.setBreakType(brk.type().name());
        entity.setDetail(brk.detail());
        entity.setStatus(brk.status().name());
        entity.setResolutionAction(brk.resolutionAction() == null ? null : brk.resolutionAction().name());
        entity.setResolvedBy(brk.resolvedBy());
        entity.setResolvedAt(brk.resolvedAt());
        entity.setCreatedAt(brk.createdAt());
        return entity;
    }

    static Break toBreak(BreakJpaEntity entity) {
        return Break.rehydrate(
                entity.getId(),
                entity.getRunId(),
                entity.getPaymentId(),
                entity.getExternalRef(),
                BreakType.valueOf(entity.getBreakType()),
                entity.getDetail(),
                BreakStatus.valueOf(entity.getStatus()),
                entity.getResolutionAction() == null ? null : ResolveAction.valueOf(entity.getResolutionAction()),
                entity.getResolvedBy(),
                entity.getResolvedAt(),
                entity.getCreatedAt()
        );
    }
}
