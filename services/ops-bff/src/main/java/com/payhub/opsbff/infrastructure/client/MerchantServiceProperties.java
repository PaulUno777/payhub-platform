package com.payhub.opsbff.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.clients")
public record MerchantServiceProperties(String merchantService) {

    public MerchantServiceProperties {
        if (merchantService == null || merchantService.isBlank()) {
            merchantService = "http://localhost:8300";
        }
    }
}
