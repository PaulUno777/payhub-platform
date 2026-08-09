package com.payhub.opsbff.application.port.out;

import java.util.List;
import java.util.UUID;

public interface ReconciliationPort {

    RunDto startRun(UUID tenantId, String railCode, String statementKey);

    List<BreakDto> listBreaks(UUID runId);

    BreakDto getBreak(UUID breakId);

    BreakDto resolve(UUID breakId, String action, String actor, String idempotencyKey);

    record RunDto(
            UUID id,
            UUID tenantId,
            String railCode,
            String statementKey,
            String status,
            String createdAt
    ) {
    }

    record BreakDto(
            UUID id,
            UUID runId,
            UUID paymentId,
            String externalRef,
            String type,
            String detail,
            String status,
            String resolutionAction,
            String resolvedBy,
            String resolvedAt,
            String createdAt
    ) {
    }
}
