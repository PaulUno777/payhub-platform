package com.payhub.reporting.infrastructure.messaging;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.payhub.messaging.kafka.KafkaPoisonHandlers;
import com.payhub.reporting.application.port.out.DlqReplayPort;

@Component
@ConditionalOnProperty(prefix = "payhub.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaDlqReplayAdapter implements DlqReplayPort {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);
    private static final int MAX_EMPTY_POLLS = 5;

    private final KafkaConsumerProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaDlqReplayAdapter(KafkaConsumerProperties properties, KafkaTemplate<String, String> kafkaTemplate) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public int republish(String dlqTopic, int maxRecords) {
        String originalTopic = KafkaPoisonHandlers.originalTopicFromDlq(dlqTopic);
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "reporting-dlq-replay");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        int republished = 0;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(dlqTopic));
            int emptyPolls = 0;
            while (republished < maxRecords && emptyPolls < MAX_EMPTY_POLLS) {
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }
                emptyPolls = 0;
                for (ConsumerRecord<String, String> record : records) {
                    if (republished >= maxRecords) {
                        break;
                    }
                    ProducerRecord<String, String> outbound = new ProducerRecord<>(
                            originalTopic,
                            null,
                            record.key(),
                            record.value(),        stripDltHeaders(record)
                    );
                    try {
                        kafkaTemplate.send(outbound).get(10, TimeUnit.SECONDS);
                        republished++;
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to republish DLQ record offset=" + record.offset(), e);
                    }
                }
                consumer.commitSync();
            }
        }
        return republished;
    }

    private static RecordHeaders stripDltHeaders(ConsumerRecord<String, String> record) {
        RecordHeaders headers = new RecordHeaders();
        for (Header header : record.headers()) {
            String key = header.key();
            if (key != null && (key.startsWith("kafka_dlt-") || key.startsWith("kafka_exception"))) {
                continue;
            }
            headers.add(header);
        }
        return headers;
    }
}
