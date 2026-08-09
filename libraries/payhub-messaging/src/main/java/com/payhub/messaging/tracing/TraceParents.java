package com.payhub.messaging.tracing;

import java.util.function.Supplier;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

/**
 * W3C {@code traceparent} helpers for Kafka envelopes and non-HTTP hops.
 * Technical only — not a shared domain type.
 */
public final class TraceParents {

    private TraceParents() {
    }

    /**
     * Current span as W3C {@code traceparent}, or {@code null} when no span is active.
     */
    public static String current(Tracer tracer) {
        if (tracer == null) {
            return null;
        }
        Span span = tracer.currentSpan();
        if (span == null) {
            return null;
        }
        return format(span.context());
    }

    public static String format(TraceContext context) {
        if (context == null || context.traceId() == null || context.spanId() == null) {
            return null;
        }
        String traceId = normalizeTraceId(context.traceId());
        String spanId = context.spanId();
        if (traceId == null || spanId.length() != 16) {
            return null;
        }
        String flags = Boolean.TRUE.equals(context.sampled()) ? "01" : "00";
        return "00-" + traceId + "-" + spanId + "-" + flags;
    }

    /**
     * Extracts the 32-hex trace id from a W3C {@code traceparent}, or {@code null} if invalid.
     */
    public static String traceId(String traceparent) {
        Parsed parsed = parse(traceparent);
        return parsed == null ? null : parsed.traceId();
    }

    /**
     * Runs {@code action} inside a child span whose parent is taken from {@code traceparent}
     * when present and valid; otherwise starts a root span.
     */
    public static void withContinuedSpan(Tracer tracer, String traceparent, String spanName, Runnable action) {
        withContinuedSpan(tracer, traceparent, spanName, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T withContinuedSpan(Tracer tracer, String traceparent, String spanName, Supplier<T> action) {
        Span span = startContinuedSpan(tracer, traceparent, spanName);
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return action.get();
        }
        finally {
            span.end();
        }
    }

    public static Span startContinuedSpan(Tracer tracer, String traceparent, String spanName) {
        Span.Builder builder = tracer.spanBuilder().name(spanName).kind(Span.Kind.CONSUMER);
        Parsed parent = parse(traceparent);
        if (parent != null) {
            TraceContext parentContext = tracer.traceContextBuilder()
                    .traceId(parent.traceId())
                    .spanId(parent.spanId())
                    .sampled(parent.sampled())
                    .build();
            builder.setParent(parentContext);
        }
        else {
            builder.setNoParent();
        }
        return builder.start();
    }

    /**
     * Pads 16-hex (Zipkin / SimpleTracer) trace ids to W3C 32-hex.
     */
    public static String normalizeTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return null;
        }
        if (traceId.length() == 32) {
            return traceId;
        }
        if (traceId.length() == 16) {
            return "0000000000000000" + traceId;
        }
        return null;
    }

    private static Parsed parse(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return null;
        }
        String[] parts = traceparent.trim().split("-");
        if (parts.length != 4 || !"00".equals(parts[0])) {
            return null;
        }
        String traceId = normalizeTraceId(parts[1]);
        if (traceId == null || parts[2].length() != 16 || parts[3].length() != 2) {
            return null;
        }
        boolean sampled = parts[3].endsWith("1");
        return new Parsed(traceId, parts[2], sampled);
    }

    private record Parsed(String traceId, String spanId, boolean sampled) {
    }
}
