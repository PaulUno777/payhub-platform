package com.payhub.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.payhub.notification.application.port.out.WebhookDeliveryRepository;
import com.payhub.notification.domain.WebhookDelivery;
import com.payhub.notification.domain.WebhookDeliveryStatus;

@Component
public class JpaWebhookDeliveryRepository implements WebhookDeliveryRepository {

    private static final EnumSet<WebhookDeliveryStatus> DUE_STATUSES = EnumSet.of(
            WebhookDeliveryStatus.PENDING,
            WebhookDeliveryStatus.FAILED_RETRYABLE
    );

    private final WebhookDeliveryJpaRepository jpaRepository;

    public JpaWebhookDeliveryRepository(WebhookDeliveryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(WebhookDelivery delivery) {
        WebhookDeliveryEntity entity = jpaRepository.findById(delivery.id())
                .orElseGet(() -> WebhookDeliveryEntity.fromDomain(delivery));
        entity.apply(delivery);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<WebhookDelivery> findById(UUID id) {
        return jpaRepository.findById(id).map(entity -> entity.toDomain());
    }

    @Override
    public List<WebhookDelivery> findDue(Instant now, int limit) {
        return jpaRepository.findDue(DUE_STATUSES, now, PageRequest.of(0, limit)).stream()
                .map(entity -> entity.toDomain())
                .toList();
    }

    @Override
    public List<WebhookDelivery> findDeadByTenant(UUID tenantId, int limit) {
        return jpaRepository.findByTenantIdAndStatusOrderByUpdatedAtDesc(
                        tenantId,
                        WebhookDeliveryStatus.DEAD,
                        PageRequest.of(0, limit)
                ).stream()
                .map(entity -> entity.toDomain())
                .toList();
    }
}
