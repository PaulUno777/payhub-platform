package com.payhub.opsbff.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.clients")
public record MerchantServiceProperties(
        String merchantService,
        String paymentOrchestrator,
        String reconciliationService,
        String reportingService,
        String notificationService
) {

    public MerchantServiceProperties {
        if (merchantService == null || merchantService.isBlank()) {
            merchantService = "http://localhost:8300";
        }
        if (paymentOrchestrator == null || paymentOrchestrator.isBlank()) {
            paymentOrchestrator = "http://localhost:8310";
        }
        if (reconciliationService == null || reconciliationService.isBlank()) {
            reconciliationService = "http://localhost:8400";
        }
        if (reportingService == null || reportingService.isBlank()) {
            reportingService = "http://localhost:8410";
        }
        if (notificationService == null || notificationService.isBlank()) {
            notificationService = "http://localhost:8420";
        }
    }
}
