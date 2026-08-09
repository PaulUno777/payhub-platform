package com.payhub.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.payhub.notification.domain.WebhookDelivery;
import com.payhub.notification.domain.WebhookDeliveryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "webhook_delivery")
public class WebhookDeliveryEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "payment_status", nullable = false, length = 64)
    private String paymentStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "endpoint_url", nullable = false, length = 2048)
    private String endpointUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WebhookDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WebhookDeliveryEntity() {
    }

    public static WebhookDeliveryEntity fromDomain(WebhookDelivery delivery) {
        WebhookDeliveryEntity entity = new WebhookDeliveryEntity();
        entity.apply(delivery);
        return entity;
    }

    public void apply(WebhookDelivery delivery) {
        this.id = delivery.id();
        this.eventId = delivery.eventId();
        this.paymentId = delivery.paymentId();
        this.tenantId = delivery.tenantId();
        this.paymentStatus = delivery.paymentStatus();
        this.occurredAt = delivery.occurredAt();
        this.endpointUrl = delivery.endpointUrl() == null ? "" : delivery.endpointUrl();
        this.status = delivery.status();
        this.attemptCount = delivery.attemptCount();
        this.nextAttemptAt = delivery.nextAttemptAt();
        this.lastError = delivery.lastError();
        this.createdAt = delivery.createdAt();
        this.updatedAt = delivery.updatedAt();
    }

    public WebhookDelivery toDomain() {
        return WebhookDelivery.rehydrate(
                id,
                eventId,
                paymentId,
                tenantId,
                paymentStatus,
                occurredAt,
                endpointUrl,
                status,
                attemptCount,
                nextAttemptAt,
                lastError,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public WebhookDeliveryStatus getStatus() {
        return status;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
