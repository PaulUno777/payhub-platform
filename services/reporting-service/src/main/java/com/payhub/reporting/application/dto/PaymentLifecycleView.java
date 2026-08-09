package com.payhub.reporting.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Non-authoritative payment lifecycle read model. Always carries freshness metadata.
 */
public record PaymentLifecycleView(
        UUID paymentId,
        UUID tenantId,
        UUID merchantId,
        String status,
        Instant occurredAt,
        Instant asOf,
        long stalenessMs,
        Freshness freshness
) {

    public enum Freshness {
        /** CQRS projection — never claim live / financial truth. */
        NON_AUTHORITATIVE
    }
}
