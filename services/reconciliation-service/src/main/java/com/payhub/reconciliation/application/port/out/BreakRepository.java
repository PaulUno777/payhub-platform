package com.payhub.reconciliation.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payhub.reconciliation.domain.Break;
import com.payhub.reconciliation.domain.BreakStatus;

public interface BreakRepository {

    Break save(Break brk);

    Optional<Break> findById(UUID id);

    List<Break> findByRunId(UUID runId);

    List<Break> findByStatus(BreakStatus status);
}
