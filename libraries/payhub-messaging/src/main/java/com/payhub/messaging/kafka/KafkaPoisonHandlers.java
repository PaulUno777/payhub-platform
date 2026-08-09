package com.payhub.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Shared Kafka consumer error handling for DS-016: finite in-listener retries, then
 * publish to {@code {originalTopic}.dlq} (plan §6.1 naming — not Spring's default {@code .DLT}).
 */
public final class KafkaPoisonHandlers {

    public static final String DLQ_SUFFIX = ".dlq";
    public static final long DEFAULT_RETRY_INTERVAL_MS = 500L;
    public static final long DEFAULT_MAX_ATTEMPTS = 3L;

    private KafkaPoisonHandlers() {
    }

    public static String dlqTopic(String originalTopic) {
        if (originalTopic == null || originalTopic.isBlank()) {
            throw new IllegalArgumentException("originalTopic required");
        }
        if (originalTopic.endsWith(DLQ_SUFFIX)) {
            return originalTopic;
        }
        return originalTopic + DLQ_SUFFIX;
    }

    public static String originalTopicFromDlq(String dlqTopic) {
        if (dlqTopic == null || dlqTopic.isBlank()) {
            throw new IllegalArgumentException("dlqTopic required");
        }
        if (!dlqTopic.endsWith(DLQ_SUFFIX)) {
            throw new IllegalArgumentException("dlqTopic must end with " + DLQ_SUFFIX);
        }
        return dlqTopic.substring(0, dlqTopic.length() - DLQ_SUFFIX.length());
    }

    public static DeadLetterPublishingRecoverer deadLetterRecoverer(KafkaTemplate<String, String> template) {
        return new DeadLetterPublishingRecoverer(template, KafkaPoisonHandlers::dlqDestination);
    }

    /**
     * Finite retries for transient failures; poison / parse failures go straight to {@code .dlq}.
     *
     * @param additionalNotRetryable extra exception types (e.g. Jackson parse errors) to skip retries
     */
    @SafeVarargs
    public static DefaultErrorHandler defaultErrorHandler(
            KafkaTemplate<String, String> template,
            Class<? extends Exception>... additionalNotRetryable
    ) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                deadLetterRecoverer(template),
                new FixedBackOff(DEFAULT_RETRY_INTERVAL_MS, DEFAULT_MAX_ATTEMPTS)
        );
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                SerializationException.class,
                IllegalArgumentException.class
        );
        if (additionalNotRetryable != null) {
            for (Class<? extends Exception> type : additionalNotRetryable) {
                if (type != null) {
                    handler.addNotRetryableExceptions(type);
                }
            }
        }
        return handler;
    }

    private static TopicPartition dlqDestination(ConsumerRecord<?, ?> record, Exception exception) {
        return new TopicPartition(dlqTopic(record.topic()), record.partition());
    }
}
