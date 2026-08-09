package com.payhub.notification.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEndpointPort {

    Optional<String> resolveUrl(UUID tenantId);
}
