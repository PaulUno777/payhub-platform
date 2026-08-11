package com.payhub.reporting.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.payhub.messaging.kafka.KafkaPoisonHandlers;
import com.payhub.reporting.application.dto.JournalEntryEnvelope;
import com.payhub.reporting.application.dto.JournalEntryEnvelope.PostingSummary;
import com.payhub.reporting.application.dto.JournalEntryEnvelope.TransactionPostedPayload;
import com.payhub.reporting.application.port.out.JournalEntryProjectionStore;
import com.redis.testcontainers.RedisContainer;

import tools.jackson.databind.ObjectMapper;

/**
 * DS-016 exit: poison on one partition must not block processing on another.
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PoisonMessageDoesNotBlockOtherPartitionsIT {

    /** Unique topic so listeners cannot auto-create a 1-partition topic before AdminClient runs. */
    private static final String TOPIC = "ledger.journal-entry.poison-it.v1";
    private static final String DLQ = KafkaPoisonHandlers.dlqTopic(TOPIC);

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1"));

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));

    private final JournalEntryProjectionStore projectionStore;
    private final ObjectMapper objectMapper;

    PoisonMessageDoesNotBlockOtherPartitionsIT(
            JournalEntryProjectionStore projectionStore,
            ObjectMapper objectMapper
    ) {
        this.projectionStore = projectionStore;
        this.objectMapper = objectMapper;
    }

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) throws Exception {
        ensureTwoPartitionTopics();
        registry.add("payhub.kafka.enabled", () -> "true");
        registry.add("payhub.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("payhub.kafka.journal-entry-topic", () -> TOPIC);
        registry.add("payhub.kafka.consumer-group", () -> "reporting-poison-it-" + UUID.randomUUID());
        registry.add("payhub.kafka.concurrency", () -> "2");
        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redis.getRedisPort()));
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    private static void ensureTwoPartitionTopics() throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (AdminClient admin = AdminClient.create(props)) {
            try {
                admin.createTopics(List.of(
                        new NewTopic(TOPIC, 2, (short) 1),
                        new NewTopic(DLQ, 2, (short) 1)
                )).all().get(30, TimeUnit.SECONDS);
            } catch (Exception ex) {
                if (!(rootCause(ex) instanceof TopicExistsException)) {
                    throw ex;
                }
            }
        }
    }

    private static Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c;
    }

    @Test
    void should_isolate_poison_on_partition_zero_without_blocking_partition_one() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        Instant occurred = Instant.parse("2026-08-09T16:00:00Z");
        JournalEntryEnvelope envelope = new JournalEntryEnvelope(
                eventId,
                "TransactionPosted",
                "1",
                occurred,
                "finledger",
                journalId,
                tenantId,
                null,
                null,
                new TransactionPostedPayload(
                        tenantId,
                        journalId,
                        "tx-poison-isolation",
                        "TRANSFER",
                        List.of(new PostingSummary(UUID.randomUUID(), "10.00", "USD", "SETTLED")),
                        occurred
                )
        );
        String validJson = objectMapper.writeValueAsString(envelope);
        String poisonJson = "{not-json";

        KafkaTemplate<String, String> template = producer();
        template.send(new ProducerRecord<>(TOPIC, 0, "poison-key", poisonJson)).get(10, TimeUnit.SECONDS);
        template.send(new ProducerRecord<>(TOPIC, 1, journalId.toString(), validJson)).get(10, TimeUnit.SECONDS);
        template.flush();

        JournalEntryProjectionStore.JournalEntryProjection projection = null;
        for (int i = 0; i < 60; i++) {
            projection = projectionStore.findByJournalEntryId(journalId).orElse(null);
            if (projection != null) {
                break;
            }
            Thread.sleep(250);
        }
        assertThat(projection)
                .as("valid message on partition 1 must be projected despite poison on partition 0")
                .isNotNull();
        assertThat(java.util.Objects.requireNonNull(projection).eventId()).isEqualTo(eventId);

        ConsumerRecord<String, String> dlqRecord = awaitDlqRecord(poisonJson);
        assertThat(dlqRecord.value()).isEqualTo(poisonJson);
        assertThat(dlqRecord.partition()).isEqualTo(0);
    }

    private ConsumerRecord<String, String> awaitDlqRecord(String expectedValue) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-assert-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(DLQ));
            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (System.nanoTime() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (expectedValue.equals(record.value())) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("Poison message did not appear on " + DLQ);
    }

    private KafkaTemplate<String, String> producer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
