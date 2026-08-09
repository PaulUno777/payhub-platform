package com.payhub.reporting.application.port.out;

import java.util.UUID;

public interface InboxStore {

    boolean exists(UUID eventId);

    void save(UUID eventId, String consumerName);
}
