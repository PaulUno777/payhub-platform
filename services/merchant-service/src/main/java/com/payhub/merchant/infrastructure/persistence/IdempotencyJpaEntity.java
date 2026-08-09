package com.payhub.merchant.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_record")
@IdClass(IdempotencyId.class)
public class IdempotencyJpaEntity {

    @Id
    @Column(nullable = false, length = 64)
    private String operation;

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 256)
    private String requestHash;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyJpaEntity() {
    }

    public IdempotencyJpaEntity(
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID merchantId,
            Instant createdAt
    ) {
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.merchantId = merchantId;
        this.createdAt = createdAt;
    }

    public String getOperation() { return operation; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public UUID getMerchantId() { return merchantId; }
    public Instant getCreatedAt() { return createdAt; }
}
