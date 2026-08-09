package com.payhub.orchestrator.application.dto;

import java.util.UUID;

public record ResolveReconciliationCommand(
        UUID paymentId,
        String action,
        String idempotencyKey
) {
}
