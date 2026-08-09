package com.payhub.messaging.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;

@Tag("unit")
class TraceParentsTest {

    @Test
    void should_format_and_continue_same_trace_id_across_kafka_hop() {
        SimpleTracer publisherTracer = new SimpleTracer();
        Span root = publisherTracer.nextSpan().name("http.handle").start();
        String envelopeTraceparent;
        try (Tracer.SpanInScope ignored = publisherTracer.withSpan(root)) {
            envelopeTraceparent = TraceParents.current(publisherTracer);
        }
        finally {
            root.end();
        }

        assertThat(envelopeTraceparent).isNotBlank();
        String publishedTraceId = TraceParents.traceId(envelopeTraceparent);
        assertThat(publishedTraceId).hasSize(32);
        assertThat(publishedTraceId).endsWith(root.context().traceId());

        SimpleTracer consumerTracer = new SimpleTracer();
        String[] consumerTraceId = new String[1];
        TraceParents.withContinuedSpan(consumerTracer, envelopeTraceparent, "payment.lifecycle.consume", () -> {
            Span span = consumerTracer.currentSpan();
            if (span == null) {
                throw new AssertionError("expected active consumer span");
            }
            consumerTraceId[0] = TraceParents.normalizeTraceId(span.context().traceId());
        });

        assertThat(consumerTraceId[0]).isEqualTo(publishedTraceId);
    }

    @Test
    void should_return_null_when_no_active_span() {
        assertThat(TraceParents.current(new SimpleTracer())).isNull();
        assertThat(TraceParents.traceId(null)).isNull();
        assertThat(TraceParents.traceId("not-a-traceparent")).isNull();
    }

    @Test
    void should_pad_16_hex_trace_id_to_w3c_32_hex() {
        assertThat(TraceParents.normalizeTraceId("899524db60efe114"))
                .isEqualTo("0000000000000000899524db60efe114");
    }
}
