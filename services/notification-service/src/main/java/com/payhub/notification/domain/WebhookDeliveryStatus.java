package com.payhub.notification.domain;

public enum WebhookDeliveryStatus {
    PENDING,
    IN_FLIGHT,
    DELIVERED,
    FAILED_RETRYABLE,
    DEAD
}
