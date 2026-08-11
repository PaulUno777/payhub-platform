package com.payhub.reporting.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerConfig;
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

import com.payhub.reporting.application.dto.JournalEntryEnvelope;
import com.payhub.reporting.application.dto.JournalEntryEnvelope.PostingSummary;
import com.payhub.reporting.application.dto.JournalEntryEnvelope.TransactionPostedPayload;
import com.payhub.reporting.application.port.out.JournalEntryProjectionStore;
import com.payhub.reporting.infrastructure.messaging.KafkaConsumerProperties;
import com.redis.testcontainers.RedisContainer;

import tools.jackson.databind.ObjectMapper;

@Tag("integration")
@SpringBootTest
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JournalEntryKafkaDuplicateDeliveryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1"));

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));

    private final JournalEntryProjectionStore projectionStore;
    private final ObjectMapper objectMapper;
    private final KafkaConsumerProperties kafkaConsumerProperties;

    JournalEntryKafkaDuplicateDeliveryTest(
            JournalEntryProjectionStore projectionStore,
            ObjectMapper objectMapper,
            KafkaConsumerProperties kafkaConsumerProperties
    ) {
        this.projectionStore = projectionStore;
        this.objectMapper = objectMapper;
        this.kafkaConsumerProperties = kafkaConsumerProperties;
    }

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("payhub.kafka.enabled", () -> "true");
        registry.add("payhub.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("payhub.kafka.journal-entry-topic", () -> "ledger.journal-entry.v1");
        registry.add("payhub.kafka.consumer-group", () -> "reporting-it-" + UUID.randomUUID());
        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redis.getRedisPort()));
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Test
    void should_apply_projection_once_when_same_event_published_twice() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        Instant occurred = Instant.parse("2026-08-09T01:00:00Z");
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
                        "tx-dup",
                        "TRANSFER",
                        List.of(new PostingSummary(UUID.randomUUID(), "25.00", "USD", "SETTLED")),
                        occurred
                )
        );
        String json = objectMapper.writeValueAsString(envelope);

        KafkaTemplate<String, String> template = producer();
        template.send(kafkaConsumerProperties.journalEntryTopic(), journalId.toString(), json).get(10, TimeUnit.SECONDS);
        template.send(kafkaConsumerProperties.journalEntryTopic(), journalId.toString(), json).get(10, TimeUnit.SECONDS);
        template.flush();

        JournalEntryProjectionStore.JournalEntryProjection projection = null;
        for (int i = 0; i < 60; i++) {
            projection = projectionStore.findByJournalEntryId(journalId).orElse(null);
            if (projection != null) {
                break;
            }
            Thread.sleep(500);
        }
        assertThat(projection).isNotNull();
        Thread.sleep(1500);
        assertThat(projectionStore.count()).isEqualTo(1);
        assertThat(projectionStore.findByJournalEntryId(journalId).orElseThrow().eventId()).isEqualTo(eventId);
    }

    private KafkaTemplate<String, String> producer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
