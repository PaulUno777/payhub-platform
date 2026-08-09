package com.payhub.reporting.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_lifecycle_projection")
public class PaymentLifecycleProjectionEntity {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "as_of", nullable = false)
    private Instant asOf;

    protected PaymentLifecycleProjectionEntity() {
    }

    public PaymentLifecycleProjectionEntity(
            UUID paymentId,
            UUID tenantId,
            UUID merchantId,
            UUID eventId,
            String status,
            Instant occurredAt,
            Instant asOf
    ) {
        this.paymentId = paymentId;
        this.tenantId = tenantId;
        this.merchantId = merchantId;
        this.eventId = eventId;
        this.status = status;
        this.occurredAt = occurredAt;
        this.asOf = asOf;
    }

    public UUID getPaymentId() { return paymentId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMerchantId() { return merchantId; }
    public UUID getEventId() { return eventId; }
    public String getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getAsOf() { return asOf; }

    public void apply(
            UUID tenantId,
            UUID merchantId,
            UUID eventId,
            String status,
            Instant occurredAt,
            Instant asOf
    ) {
        this.tenantId = tenantId;
        this.merchantId = merchantId;
        this.eventId = eventId;
        this.status = status;
        this.occurredAt = occurredAt;
        this.asOf = asOf;
    }
}
