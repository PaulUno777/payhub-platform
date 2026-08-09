package com.payhub.reporting.adapter.in.event;

import java.io.IOException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope;
import com.payhub.reporting.application.port.in.ApplyPaymentLifecycleProjectionUseCase;

import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "payhub.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentLifecycleKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentLifecycleKafkaListener.class);

    private final ApplyPaymentLifecycleProjectionUseCase applyPaymentLifecycleProjectionUseCase;
    private final ObjectMapper objectMapper;

    public PaymentLifecycleKafkaListener(
            ApplyPaymentLifecycleProjectionUseCase applyPaymentLifecycleProjectionUseCase,
            ObjectMapper objectMapper
    ) {
        this.applyPaymentLifecycleProjectionUseCase = applyPaymentLifecycleProjectionUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${payhub.kafka.payment-lifecycle-topic:payment.lifecycle.v1}",
            groupId = "${payhub.kafka.payment-lifecycle-consumer-group:reporting-payment-lifecycle-v1}"
    )
    public void onMessage(ConsumerRecord<String, String> record) throws IOException {
        PaymentLifecycleEnvelope envelope = objectMapper.readValue(record.value(), PaymentLifecycleEnvelope.class);
        boolean applied = applyPaymentLifecycleProjectionUseCase.execute(envelope);
        if (!applied) {
            log.debug("Skipped duplicate payment lifecycle eventId={}", envelope.eventId());
        }
    }
}
