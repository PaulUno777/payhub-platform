package com.payhub.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class KafkaPoisonHandlersTest {

    @Test
    void should_append_dlq_suffix_to_original_topic() {
        assertThat(KafkaPoisonHandlers.dlqTopic("ledger.journal-entry.v1"))
                .isEqualTo("ledger.journal-entry.v1.dlq");
        assertThat(KafkaPoisonHandlers.dlqTopic("payment.lifecycle.v1"))
                .isEqualTo("payment.lifecycle.v1.dlq");
    }

    @Test
    void should_not_double_append_dlq_suffix() {
        assertThat(KafkaPoisonHandlers.dlqTopic("ledger.journal-entry.v1.dlq"))
                .isEqualTo("ledger.journal-entry.v1.dlq");
    }

    @Test
    void should_derive_original_topic_from_dlq() {
        assertThat(KafkaPoisonHandlers.originalTopicFromDlq("ledger.journal-entry.v1.dlq"))
                .isEqualTo("ledger.journal-entry.v1");
    }

    @Test
    void should_reject_blank_or_non_dlq_topics() {
        assertThatThrownBy(() -> KafkaPoisonHandlers.dlqTopic(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KafkaPoisonHandlers.originalTopicFromDlq("ledger.journal-entry.v1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
