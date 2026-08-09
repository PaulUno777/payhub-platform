package com.payhub.notification.infrastructure.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.notification.webhooks")
public record NotificationWebhookProperties(
        String defaultUrl,
        Map<String, String> tenantUrls,
        String hmacSecret,
        int maxAttempts,
        Duration baseBackoff,
        int dispatchBatchSize,
        List<String> notifyStatuses
) {

    public NotificationWebhookProperties {
        if (tenantUrls == null) {
            tenantUrls = Map.of();
        }
        if (hmacSecret == null || hmacSecret.isBlank()) {
            hmacSecret = "local-dev-webhook-hmac-secret-change-me";
        }
        if (maxAttempts < 1) {
            maxAttempts = 5;
        }
        if (baseBackoff == null) {
            baseBackoff = Duration.ofSeconds(2);
        }
        if (dispatchBatchSize < 1) {
            dispatchBatchSize = 20;
        }
        if (notifyStatuses == null || notifyStatuses.isEmpty()) {
            notifyStatuses = List.of(
                    "SETTLED",
                    "FAILED_FINAL",
                    "REFUND_SETTLED",
                    "REFUND_FAILED_FINAL"
            );
        }
    }
}
