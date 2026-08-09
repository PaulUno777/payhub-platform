package com.payhub.notification.application.usecase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.notification.application.dto.WebhookDeliveryView;
import com.payhub.notification.application.port.in.ListDeadLetterWebhooksUseCase;
import com.payhub.notification.application.port.out.WebhookDeliveryRepository;

@Service
public class ListDeadLetterWebhooksService implements ListDeadLetterWebhooksUseCase {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final WebhookDeliveryRepository deliveryRepository;

    public ListDeadLetterWebhooksService(WebhookDeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookDeliveryView> execute(UUID tenantId, int limit) {
        Objects.requireNonNull(tenantId, "tenantId");
        int capped = limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return deliveryRepository.findDeadByTenant(tenantId, capped).stream()
                .map(WebhookDeliveryView::from)
                .toList();
    }
}
