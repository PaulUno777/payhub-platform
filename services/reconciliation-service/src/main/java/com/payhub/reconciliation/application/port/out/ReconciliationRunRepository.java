package com.payhub.reconciliation.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payhub.reconciliation.domain.ReconciliationRun;
import com.payhub.reconciliation.domain.RunStatus;

public interface ReconciliationRunRepository {

    ReconciliationRun save(ReconciliationRun run);

    Optional<ReconciliationRun> findById(UUID id);

    Optional<ReconciliationRun> findOpen(UUID tenantId, String railCode);

    List<ReconciliationRun> findByStatus(RunStatus status);
}
