package com.payhub.reconciliation.application.port.in;

import java.util.UUID;

import com.payhub.reconciliation.application.dto.ReconciliationRunView;

public interface StartReconciliationRunUseCase {

    ReconciliationRunView execute(Command command);

    record Command(UUID tenantId, String railCode, String statementKey) {
    }
}
