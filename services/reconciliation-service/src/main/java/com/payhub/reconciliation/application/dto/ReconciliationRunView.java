package com.payhub.reconciliation.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.payhub.reconciliation.domain.ReconciliationRun;

public record ReconciliationRunView(
        UUID id,
        UUID tenantId,
        String railCode,
        String statementKey,
        String status,
        Instant createdAt
) {
    public static ReconciliationRunView from(ReconciliationRun run) {
        return new ReconciliationRunView(
                run.id(),
                run.tenantId(),
                run.railCode(),
                run.statementKey(),
                run.status().name(),
                run.createdAt()
        );
    }
}
