package com.payhub.opsbff.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.payhub.opsbff.application.port.in.NotificationOpsUseCase;
import com.payhub.opsbff.application.port.out.NotificationPort;
import com.payhub.opsbff.application.port.out.NotificationPort.WebhookDeliveryDto;

@Service
public class NotificationOpsService implements NotificationOpsUseCase {

    private final NotificationPort notificationPort;

    public NotificationOpsService(NotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    @Override
    public List<WebhookDeliveryDto> listDeadLetterQueue(UUID tenantId, int limit) {
        return notificationPort.listDeadLetterQueue(tenantId, limit);
    }
}
