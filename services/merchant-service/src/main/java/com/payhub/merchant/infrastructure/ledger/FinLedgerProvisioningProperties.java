package com.payhub.merchant.infrastructure.ledger;

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.finledger")
public record FinLedgerProvisioningProperties(
        String baseUrl,
        UUID aggregatorTenantId,
        String defaultCurrency
) {

    public FinLedgerProvisioningProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8080";
        }
        if (aggregatorTenantId == null) {
            aggregatorTenantId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        }
        if (defaultCurrency == null || defaultCurrency.isBlank()) {
            defaultCurrency = "USD";
        }
    }
}
