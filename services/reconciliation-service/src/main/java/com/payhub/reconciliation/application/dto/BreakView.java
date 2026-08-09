package com.payhub.reconciliation.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.payhub.reconciliation.domain.Break;

public record BreakView(
        UUID id,
        UUID runId,
        UUID paymentId,
        String externalRef,
        String type,
        String detail,
        String status,
        String resolutionAction,
        String resolvedBy,
        Instant resolvedAt,
        Instant createdAt
) {
    public static BreakView from(Break brk) {
        return new BreakView(
                brk.id(),
                brk.runId(),
                brk.paymentId(),
                brk.externalRef(),
                brk.type().name(),
                brk.detail(),
                brk.status().name(),
                brk.resolutionAction() == null ? null : brk.resolutionAction().name(),
                brk.resolvedBy(),
                brk.resolvedAt(),
                brk.createdAt()
        );
    }
}
