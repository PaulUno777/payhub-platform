package com.payhub.orchestrator.infrastructure.ledger;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties(prefix = "payhub.finledger")
public record FinLedgerProperties(
        String baseUrl,
        String bearerToken,
        String railCode,
        UUID clearingAccountId,
        UUID counterpartyAccountId,
        UUID feeConfigTenantId,
        boolean provisionFeeConfig
) {

    public FinLedgerProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8080";
        }
        if (bearerToken == null) {
            bearerToken = "";
        }
        if (railCode == null || railCode.isBlank()) {
            railCode = "MANUAL";
        }
        if (clearingAccountId == null) {
            clearingAccountId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        }
        if (counterpartyAccountId == null) {
            counterpartyAccountId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        }
        if (feeConfigTenantId == null) {
            feeConfigTenantId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        }
    }
}
