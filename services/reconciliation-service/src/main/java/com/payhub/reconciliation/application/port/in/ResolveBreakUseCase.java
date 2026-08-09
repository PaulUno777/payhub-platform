package com.payhub.reconciliation.application.port.in;

import java.util.UUID;

import com.payhub.reconciliation.application.dto.BreakView;

public interface ResolveBreakUseCase {

    BreakView execute(Command command);

    record Command(UUID breakId, String action, String actor, String idempotencyKey) {
    }
}
