package com.payhub.reporting.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.payhub.messaging.tracing.TraceParents;
import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope;
import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope.PaymentStatusChangedPayload;
import com.payhub.reporting.application.port.in.ApplyPaymentLifecycleProjectionUseCase;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;
import tools.jackson.databind.ObjectMapper;

@Tag("unit")
class PaymentLifecycleKafkaListenerTraceTest {

    @Test
    void should_continue_consumer_span_from_envelope_traceparent() throws Exception {
        SimpleTracer publisherTracer = new SimpleTracer();
        Span root = publisherTracer.nextSpan().name("orchestrator").start();
        String traceparent;
        try (Tracer.SpanInScope ignored = publisherTracer.withSpan(root)) {
            traceparent = TraceParents.current(publisherTracer);
        }
        finally {
            root.end();
        }
        String expectedTraceId = TraceParents.traceId(traceparent);

        SimpleTracer consumerTracer = new SimpleTracer();
        AtomicReference<String> activeTraceId = new AtomicReference<>();
        ApplyPaymentLifecycleProjectionUseCase useCase = mock(ApplyPaymentLifecycleProjectionUseCase.class);
        when(useCase.execute(any())).thenAnswer(invocation -> {
            activeTraceId.set(consumerTracer.currentSpan().context().traceId());
            return true;
        });

        ObjectMapper objectMapper = new ObjectMapper();
        PaymentLifecycleKafkaListener listener = new PaymentLifecycleKafkaListener(useCase, objectMapper, consumerTracer);

        UUID paymentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        PaymentLifecycleEnvelope envelope = new PaymentLifecycleEnvelope(
                UUID.randomUUID(),
                "PaymentStatusChanged",
                "1",
                Instant.parse("2026-08-09T10:00:00Z"),
                "payment-orchestrator",
                paymentId,
                tenantId,
                traceparent,
                null,
                new PaymentStatusChangedPayload(paymentId, UUID.randomUUID(), tenantId, "SETTLED")
        );
        String json = objectMapper.writeValueAsString(envelope);
        listener.onMessage(new ConsumerRecord<>("payment.lifecycle.v1", 0, 0L, paymentId.toString(), json));

        verify(useCase).execute(any());
        assertThat(TraceParents.normalizeTraceId(activeTraceId.get())).isEqualTo(expectedTraceId);
    }
}
