package com.payhub.orchestrator.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.payhub.messaging.outbox.OutboxWriter;
import com.payhub.messaging.outbox.OutboxWriter.OutboxAppend;
import com.payhub.messaging.tracing.TraceParents;
import com.payhub.orchestrator.application.port.out.PaymentLifecyclePublisher;
import com.payhub.orchestrator.domain.PaymentStatus;

import io.micrometer.tracing.Tracer;

/**
 * Writes payment.lifecycle.v1 to the transactional outbox (same TX as caller).
 */
@Component
public class OutboxPaymentLifecyclePublisher implements PaymentLifecyclePublisher {

    public static final String EVENT_TYPE = "PaymentStatusChanged";
    public static final String AGGREGATE_TYPE = "Payment";

    private final OutboxWriter outboxWriter;
    private final Tracer tracer;
    private final String topic;

    public OutboxPaymentLifecyclePublisher(
            OutboxWriter outboxWriter,
            Tracer tracer,
            @Value("${payhub.kafka.payment-lifecycle-topic:payment.lifecycle.v1}") String topic
    ) {
        this.outboxWriter = outboxWriter;
        this.tracer = tracer;
        this.topic = topic;
    }

    @Override
    public void publishStatusChanged(UUID paymentId, UUID merchantId, UUID tenantId, PaymentStatus status) {
        Instant now = Instant.now();
        UUID eventId = UUID.randomUUID();
        String payloadJson = """
                {"paymentId":"%s","merchantId":"%s","tenantId":"%s","status":"%s"}
                """.formatted(paymentId, merchantId, tenantId, status.name()).trim();
        String traceparentJson = jsonStringOrNull(TraceParents.current(tracer));
        String envelopeJson = """
                {"eventId":"%s","eventType":"%s","schemaVersion":"1","occurredAt":"%s","producer":"payment-orchestrator","aggregateId":"%s","tenantId":"%s","traceparent":%s,"causationId":null,"payload":%s}
                """.formatted(eventId, EVENT_TYPE, now, paymentId, tenantId, traceparentJson, payloadJson).trim();

        outboxWriter.append(new OutboxAppend(
                eventId,
                AGGREGATE_TYPE,
                paymentId,
                EVENT_TYPE,
                topic,
                envelopeJson,
                tenantId,
                now
        ));
    }

    private static String jsonStringOrNull(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        return "\"" + value + "\"";
    }
}
