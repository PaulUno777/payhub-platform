package com.payhub.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.payhub.notification.domain.WebhookDeliveryStatus;

public interface WebhookDeliveryJpaRepository extends JpaRepository<WebhookDeliveryEntity, UUID> {

    @Query("""
            select w from WebhookDeliveryEntity w
            where w.status in :statuses
              and w.nextAttemptAt <= :now
            order by w.nextAttemptAt asc
            """)
    List<WebhookDeliveryEntity> findDue(
            @Param("statuses") Collection<WebhookDeliveryStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );

    List<WebhookDeliveryEntity> findByTenantIdAndStatusOrderByUpdatedAtDesc(
            UUID tenantId,
            WebhookDeliveryStatus status,
            Pageable pageable
    );
}
