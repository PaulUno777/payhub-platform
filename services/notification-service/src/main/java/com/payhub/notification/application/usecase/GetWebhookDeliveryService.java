package com.payhub.notification.application.usecase;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.notification.application.dto.WebhookDeliveryView;
import com.payhub.notification.application.port.in.GetWebhookDeliveryUseCase;
import com.payhub.notification.application.port.out.WebhookDeliveryRepository;

@Service
public class GetWebhookDeliveryService implements GetWebhookDeliveryUseCase {

    private final WebhookDeliveryRepository deliveryRepository;

    public GetWebhookDeliveryService(WebhookDeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WebhookDeliveryView> execute(UUID tenantId, UUID deliveryId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(deliveryId, "deliveryId");
        return deliveryRepository.findById(deliveryId)
                .filter(d -> d.tenantId().equals(tenantId))
                .map(WebhookDeliveryView::from);
    }
}
