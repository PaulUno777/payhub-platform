package com.payhub.messaging.inbox;

import java.util.UUID;

/**
 * Inbox-before-action: same local TX as the consumer side effect (plan §5).
 */
public interface InboxStore {

    boolean exists(UUID eventId);

    void save(UUID eventId, String consumerName);
}
