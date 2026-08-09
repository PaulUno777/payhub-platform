package com.payhub.orchestrator.application.dto;

import java.util.UUID;

public record RequestRefundCommand(
        UUID paymentId,
        String refundAmount,
        String idempotencyKey,
        String sandboxMode
) {
}
