package com.payhub.messaging.inbox;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory inbox for library unit tests. */
public final class InMemoryInboxStore implements InboxStore {

    private final Map<UUID, String> seen = new ConcurrentHashMap<>();

    @Override
    public boolean exists(UUID eventId) {
        return seen.containsKey(eventId);
    }

    @Override
    public void save(UUID eventId, String consumerName) {
        seen.putIfAbsent(eventId, consumerName);
    }

    public int size() {
        return seen.size();
    }
}
