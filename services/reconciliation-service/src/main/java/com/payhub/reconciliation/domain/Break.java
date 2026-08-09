package com.payhub.reconciliation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Break {

    private final UUID id;
    private final UUID runId;
    private final UUID paymentId;
    private final String externalRef;
    private final BreakType type;
    private final String detail;
    private BreakStatus status;
    private ResolveAction resolutionAction;
    private String resolvedBy;
    private Instant resolvedAt;
    private final Instant createdAt;

    private Break(
            UUID id,
            UUID runId,
            UUID paymentId,
            String externalRef,
            BreakType type,
            String detail,
            BreakStatus status,
            ResolveAction resolutionAction,
            String resolvedBy,
            Instant resolvedAt,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.runId = Objects.requireNonNull(runId);
        this.paymentId = Objects.requireNonNull(paymentId);
        this.externalRef = Objects.requireNonNull(externalRef);
        this.type = Objects.requireNonNull(type);
        this.detail = Objects.requireNonNull(detail);
        this.status = Objects.requireNonNull(status);
        this.resolutionAction = resolutionAction;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static Break open(
            UUID runId,
            UUID paymentId,
            String externalRef,
            BreakType type,
            String detail
    ) {
        return new Break(
                UUID.randomUUID(),
                runId,
                paymentId,
                externalRef,
                type,
                detail,
                BreakStatus.OPEN,
                null,
                null,
                null,
                Instant.now()
        );
    }

    public static Break rehydrate(
            UUID id,
            UUID runId,
            UUID paymentId,
            String externalRef,
            BreakType type,
            String detail,
            BreakStatus status,
            ResolveAction resolutionAction,
            String resolvedBy,
            Instant resolvedAt,
            Instant createdAt
    ) {
        return new Break(
                id,
                runId,
                paymentId,
                externalRef,
                type,
                detail,
                status,
                resolutionAction,
                resolvedBy,
                resolvedAt,
                createdAt
        );
    }

    public void resolve(ResolveAction action, String actor) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actor, "actor");
        if (status == BreakStatus.RESOLVED) {
            return;
        }
        if (status != BreakStatus.OPEN) {
            throw new IllegalReconciliationStateException("Cannot resolve break from status " + status);
        }
        this.status = BreakStatus.RESOLVED;
        this.resolutionAction = action;
        this.resolvedBy = actor;
        this.resolvedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID runId() { return runId; }
    public UUID paymentId() { return paymentId; }
    public String externalRef() { return externalRef; }
    public BreakType type() { return type; }
    public String detail() { return detail; }
    public BreakStatus status() { return status; }
    public ResolveAction resolutionAction() { return resolutionAction; }
    public String resolvedBy() { return resolvedBy; }
    public Instant resolvedAt() { return resolvedAt; }
    public Instant createdAt() { return createdAt; }
}
