package com.payhub.opsbff.application.port.in;

import java.util.List;
import java.util.UUID;

import com.payhub.opsbff.application.port.out.ReconciliationPort.BreakDto;
import com.payhub.opsbff.application.port.out.ReconciliationPort.RunDto;

public interface ReconciliationOpsUseCase {

    RunDto startRun(UUID tenantId, String railCode, String statementKey);

    List<BreakDto> listBreaks(UUID runId);

    BreakDto getBreak(UUID breakId);

    BreakDto resolve(UUID breakId, String action, String actor, String idempotencyKey);
}
