package com.payhub.reconciliation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ReconciliationRun {

    private final UUID id;
    private final UUID tenantId;
    private final String railCode;
    private final String statementKey;
    private RunStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private ReconciliationRun(
            UUID id,
            UUID tenantId,
            String railCode,
            String statementKey,
            RunStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.railCode = Objects.requireNonNull(railCode);
        this.statementKey = Objects.requireNonNull(statementKey);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static ReconciliationRun open(UUID tenantId, String railCode, String statementKey) {
        Instant now = Instant.now();
        return new ReconciliationRun(
                UUID.randomUUID(),
                tenantId,
                railCode,
                statementKey,
                RunStatus.OPEN,
                now,
                now
        );
    }

    public static ReconciliationRun rehydrate(
            UUID id,
            UUID tenantId,
            String railCode,
            String statementKey,
            RunStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ReconciliationRun(id, tenantId, railCode, statementKey, status, createdAt, updatedAt);
    }

    public void close() {
        if (status == RunStatus.CLOSED) {
            return;
        }
        if (status != RunStatus.OPEN) {
            throw new IllegalReconciliationStateException("Cannot close run from status " + status);
        }
        this.status = RunStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String railCode() { return railCode; }
    public String statementKey() { return statementKey; }
    public RunStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
