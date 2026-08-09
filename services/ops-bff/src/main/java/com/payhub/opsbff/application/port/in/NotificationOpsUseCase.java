package com.payhub.opsbff.application.port.in;

import java.util.List;
import java.util.UUID;

import com.payhub.opsbff.application.port.out.NotificationPort.WebhookDeliveryDto;

public interface NotificationOpsUseCase {

    List<WebhookDeliveryDto> listDeadLetterQueue(UUID tenantId, int limit);
}
