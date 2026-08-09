package com.payhub.notification.infrastructure.webhook;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.notification.application.port.out.WebhookEndpointPort;
import com.payhub.notification.infrastructure.config.NotificationWebhookProperties;

@Component
public class ConfigWebhookEndpointAdapter implements WebhookEndpointPort {

    private final NotificationWebhookProperties properties;

    public ConfigWebhookEndpointAdapter(NotificationWebhookProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> resolveUrl(UUID tenantId) {
        if (tenantId != null) {
            String override = properties.tenantUrls().get(tenantId.toString());
            if (override == null) {
                override = properties.tenantUrls().get(tenantId.toString().toLowerCase(Locale.ROOT));
            }
            if (override != null && !override.isBlank()) {
                return Optional.of(override.trim());
            }
        }
        String defaults = properties.defaultUrl();
        if (defaults == null || defaults.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(defaults.trim());
    }
}
