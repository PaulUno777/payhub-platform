package com.payhub.opsbff.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.payhub.opsbff.application.port.in.ReconciliationOpsUseCase;
import com.payhub.opsbff.application.port.out.ReconciliationPort;
import com.payhub.opsbff.application.port.out.ReconciliationPort.BreakDto;
import com.payhub.opsbff.application.port.out.ReconciliationPort.RunDto;

@Service
public class ReconciliationOpsService implements ReconciliationOpsUseCase {

    private final ReconciliationPort reconciliationPort;

    public ReconciliationOpsService(ReconciliationPort reconciliationPort) {
        this.reconciliationPort = reconciliationPort;
    }

    @Override
    public RunDto startRun(UUID tenantId, String railCode, String statementKey) {
        return reconciliationPort.startRun(tenantId, railCode, statementKey);
    }

    @Override
    public List<BreakDto> listBreaks(UUID runId) {
        return reconciliationPort.listBreaks(runId);
    }

    @Override
    public BreakDto getBreak(UUID breakId) {
        return reconciliationPort.getBreak(breakId);
    }

    @Override
    public BreakDto resolve(UUID breakId, String action, String actor, String idempotencyKey) {
        return reconciliationPort.resolve(breakId, action, actor, idempotencyKey);
    }
}
