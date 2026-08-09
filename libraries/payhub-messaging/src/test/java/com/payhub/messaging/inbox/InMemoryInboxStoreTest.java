package com.payhub.messaging.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class InMemoryInboxStoreTest {

    @Test
    void should_treat_second_save_as_already_seen() {
        InMemoryInboxStore store = new InMemoryInboxStore();
        UUID eventId = UUID.randomUUID();

        assertThat(store.exists(eventId)).isFalse();
        store.save(eventId, "consumer-a");
        assertThat(store.exists(eventId)).isTrue();
        store.save(eventId, "consumer-a");
        assertThat(store.size()).isEqualTo(1);
    }
}
