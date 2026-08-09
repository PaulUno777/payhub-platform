package com.payhub.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class EventEnvelopeTest {

    @Test
    void should_require_catalog_minimum_fields() {
        assertThatThrownBy(() -> new EventEnvelope(
                null, "PaymentStatusChanged", "1", Instant.now(), "payment-orchestrator",
                UUID.randomUUID(), UUID.randomUUID(), null, null, "{}"
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_accept_optional_trace_fields() {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                "PaymentStatusChanged",
                "1",
                Instant.parse("2026-08-09T04:00:00Z"),
                "payment-orchestrator",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "{\"status\":\"RISK_APPROVED\"}"
        );
        assertThat(envelope.eventId()).isEqualTo(eventId);
        assertThat(envelope.payloadJson()).contains("RISK_APPROVED");
    }
}
