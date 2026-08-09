package com.payhub.notification.application.port.in;

import java.util.Optional;
import java.util.UUID;

import com.payhub.notification.application.dto.WebhookDeliveryView;

public interface GetWebhookDeliveryUseCase {

    Optional<WebhookDeliveryView> execute(UUID tenantId, UUID deliveryId);
}
