package com.payhub.notification.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Outbound merchant webhook delivery aggregate (plan §1.3 / DS-014).
 * Exhausted retries land in {@link WebhookDeliveryStatus#DEAD} — never silently dropped.
 */
public final class WebhookDelivery {

    private static final int MAX_ERROR_LENGTH = 1024;

    private final UUID id;
    private final UUID eventId;
    private final UUID paymentId;
    private final UUID tenantId;
    private final String paymentStatus;
    private final Instant occurredAt;
    private final String endpointUrl;
    private WebhookDeliveryStatus status;
    private int attemptCount;
    private Instant nextAttemptAt;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private WebhookDelivery(
            UUID id,
            UUID eventId,
            UUID paymentId,
            UUID tenantId,
            String paymentStatus,
            Instant occurredAt,
            String endpointUrl,
            WebhookDeliveryStatus status,
            int attemptCount,
            Instant nextAttemptAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.endpointUrl = endpointUrl;
        this.status = Objects.requireNonNull(status, "status");
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static WebhookDelivery schedule(
            UUID eventId,
            UUID paymentId,
            UUID tenantId,
            String paymentStatus,
            Instant occurredAt,
            String endpointUrl,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        boolean missingEndpoint = endpointUrl == null || endpointUrl.isBlank();
        return new WebhookDelivery(
                UUID.randomUUID(),
                eventId,
                paymentId,
                tenantId,
                paymentStatus,
                occurredAt,
                missingEndpoint ? "" : endpointUrl.trim(),
                missingEndpoint ? WebhookDeliveryStatus.DEAD : WebhookDeliveryStatus.PENDING,
                0,
                missingEndpoint ? now : now,
                missingEndpoint ? "webhook endpoint not configured" : null,
                now,
                now
        );
    }

    public static WebhookDelivery rehydrate(
            UUID id,
            UUID eventId,
            UUID paymentId,
            UUID tenantId,
            String paymentStatus,
            Instant occurredAt,
            String endpointUrl,
            WebhookDeliveryStatus status,
            int attemptCount,
            Instant nextAttemptAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new WebhookDelivery(
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

    public void claim(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status != WebhookDeliveryStatus.PENDING && status != WebhookDeliveryStatus.FAILED_RETRYABLE) {
            throw new IllegalStateException("Cannot claim delivery in status " + status);
        }
        this.status = WebhookDeliveryStatus.IN_FLIGHT;
        this.updatedAt = now;
    }

    public void markDelivered(Instant now) {
        Objects.requireNonNull(now, "now");
        requireInFlight();
        this.attemptCount++;
        this.status = WebhookDeliveryStatus.DELIVERED;
        this.lastError = null;
        this.nextAttemptAt = now;
        this.updatedAt = now;
    }

    public void recordFailure(String error, int maxAttempts, Duration baseBackoff, Instant now) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(baseBackoff, "baseBackoff");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        requireInFlight();
        this.attemptCount++;
        this.lastError = truncate(error);
        this.updatedAt = now;
        if (this.attemptCount >= maxAttempts) {
            this.status = WebhookDeliveryStatus.DEAD;
            this.nextAttemptAt = now;
            return;
        }
        this.status = WebhookDeliveryStatus.FAILED_RETRYABLE;
        long multiplier = 1L << Math.min(this.attemptCount - 1, 20);
        this.nextAttemptAt = now.plus(baseBackoff.multipliedBy(multiplier));
    }

    private void requireInFlight() {
        if (status != WebhookDeliveryStatus.IN_FLIGHT) {
            throw new IllegalStateException("Expected IN_FLIGHT but was " + status);
        }
    }

    private static String truncate(String error) {
        if (error == null || error.isBlank()) {
            return "unknown delivery failure";
        }
        String trimmed = error.trim();
        return trimmed.length() <= MAX_ERROR_LENGTH ? trimmed : trimmed.substring(0, MAX_ERROR_LENGTH);
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID paymentId() {
        return paymentId;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String paymentStatus() {
        return paymentStatus;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public String endpointUrl() {
        return endpointUrl;
    }

    public WebhookDeliveryStatus status() {
        return status;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public String lastError() {
        return lastError;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
