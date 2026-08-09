package com.payhub.notification.application.port.in;

import java.util.List;
import java.util.UUID;

import com.payhub.notification.application.dto.WebhookDeliveryView;

public interface ListDeadLetterWebhooksUseCase {

    List<WebhookDeliveryView> execute(UUID tenantId, int limit);
}
