package com.payhub.notification.adapter.in.event;

import java.io.IOException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.payhub.messaging.tracing.TraceParents;
import com.payhub.notification.application.dto.PaymentLifecycleEnvelope;
import com.payhub.notification.application.port.in.ScheduleWebhookOnLifecycleEventUseCase;

import io.micrometer.tracing.Tracer;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "payhub.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentLifecycleKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentLifecycleKafkaListener.class);

    private final ScheduleWebhookOnLifecycleEventUseCase scheduleWebhookOnLifecycleEventUseCase;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public PaymentLifecycleKafkaListener(
            ScheduleWebhookOnLifecycleEventUseCase scheduleWebhookOnLifecycleEventUseCase,
            ObjectMapper objectMapper,
            Tracer tracer
    ) {
        this.scheduleWebhookOnLifecycleEventUseCase = scheduleWebhookOnLifecycleEventUseCase;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @KafkaListener(
            topics = "${payhub.kafka.payment-lifecycle-topic:payment.lifecycle.v1}",
            groupId = "${payhub.kafka.payment-lifecycle-consumer-group:notification-payment-lifecycle-v1}"
    )
    public void onMessage(ConsumerRecord<String, String> record) throws IOException {
        PaymentLifecycleEnvelope envelope = objectMapper.readValue(record.value(), PaymentLifecycleEnvelope.class);
        TraceParents.withContinuedSpan(tracer, envelope.traceparent(), "payment.lifecycle.consume", () -> {
            boolean scheduled = scheduleWebhookOnLifecycleEventUseCase.execute(envelope);
            if (!scheduled) {
                log.debug("Skipped payment lifecycle eventId={} (duplicate or non-notify status)", envelope.eventId());
            }
        });
    }
}
