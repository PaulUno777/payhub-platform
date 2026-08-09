package com.payhub.orchestrator.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import com.payhub.messaging.outbox.OutboxWriter;
import com.payhub.messaging.tracing.TraceParents;
import com.payhub.orchestrator.domain.PaymentStatus;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@Tag("unit")
class EndToEndTraceIdTest {

    private MockWebServer server;
    private SimpleTracer tracer;
    private RecordingOutboxWriter outbox;
    private OutboxPaymentLifecyclePublisher publisher;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        tracer = new SimpleTracer();
        outbox = new RecordingOutboxWriter();
        publisher = new OutboxPaymentLifecyclePublisher(outbox, tracer, "payment.lifecycle.v1");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void should_share_one_trace_id_across_http_client_outbox_envelope_and_consumer() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        Span root = tracer.nextSpan().name("gateway.http").start();
        String httpTraceId;
        String envelopeTraceparent;
        try (Tracer.SpanInScope ignored = tracer.withSpan(root)) {
            RestClient client = RestClient.builder()
                    .baseUrl(server.url("/").toString().replaceAll("/$", ""))
                    .requestInterceptor(traceparentInterceptor(tracer))
                    .build();
            client.get().uri("/rails/payments").retrieve().toBodilessEntity();

            RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            String outboundTraceparent = request.getHeader("traceparent");
            assertThat(outboundTraceparent).isNotBlank();
            httpTraceId = TraceParents.traceId(outboundTraceparent);
            assertThat(httpTraceId).isEqualTo(TraceParents.normalizeTraceId(root.context().traceId()));

            publisher.publishStatusChanged(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    PaymentStatus.RISK_APPROVED
            );
            assertThat(outbox.appends).hasSize(1);
            envelopeTraceparent = extractTraceparent(outbox.appends.getFirst().payloadJson());
            assertThat(TraceParents.traceId(envelopeTraceparent)).isEqualTo(httpTraceId);
        }
        finally {
            root.end();
        }

        SimpleTracer consumerTracer = new SimpleTracer();
        String[] consumerTraceId = new String[1];
        TraceParents.withContinuedSpan(consumerTracer, envelopeTraceparent, "payment.lifecycle.consume", () -> {
            consumerTraceId[0] = TraceParents.normalizeTraceId(consumerTracer.currentSpan().context().traceId());
        });
        assertThat(consumerTraceId[0]).isEqualTo(httpTraceId);
    }

    private static ClientHttpRequestInterceptor traceparentInterceptor(Tracer tracer) {
        return (request, body, execution) -> {
            String traceparent = TraceParents.current(tracer);
            if (traceparent != null) {
                request.getHeaders().set("traceparent", traceparent);
            }
            return execution.execute(request, body);
        };
    }

    private static String extractTraceparent(String envelopeJson) {
        String marker = "\"traceparent\":\"";
        int start = envelopeJson.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        int valueStart = start + marker.length();
        int valueEnd = envelopeJson.indexOf('"', valueStart);
        return envelopeJson.substring(valueStart, valueEnd);
    }

    private static final class RecordingOutboxWriter implements OutboxWriter {
        private final List<OutboxAppend> appends = new ArrayList<>();

        @Override
        public void append(OutboxAppend append) {
            appends.add(append);
        }

        @Override
        public List<OutboxRecord> findUnpublished(int limit) {
            return List.of();
        }

        @Override
        public void markPublished(UUID id, java.time.Instant publishedAt) {
        }
    }
}
