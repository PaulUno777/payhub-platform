package com.payhub.notification.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payhub.notification.domain.WebhookDelivery;

public interface WebhookDeliveryRepository {

    void save(WebhookDelivery delivery);

    Optional<WebhookDelivery> findById(UUID id);

    List<WebhookDelivery> findDue(Instant now, int limit);

    List<WebhookDelivery> findDeadByTenant(UUID tenantId, int limit);
}
