package com.payhub.notification.application;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Retry and notify-status policy for webhook dispatch (DS-014).
 */
public record WebhookDispatchPolicy(
        int maxAttempts,
        Duration baseBackoff,
        int dispatchBatchSize,
        Set<String> notifyStatuses
) {

    public WebhookDispatchPolicy {
        Objects.requireNonNull(baseBackoff, "baseBackoff");
        Objects.requireNonNull(notifyStatuses, "notifyStatuses");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (dispatchBatchSize < 1) {
            throw new IllegalArgumentException("dispatchBatchSize must be >= 1");
        }
        notifyStatuses = notifyStatuses.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean shouldNotify(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return false;
        }
        return notifyStatuses.contains(paymentStatus.toUpperCase(Locale.ROOT));
    }
}
