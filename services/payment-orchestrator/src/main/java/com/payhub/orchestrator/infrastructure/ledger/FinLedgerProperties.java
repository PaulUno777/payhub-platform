package com.payhub.orchestrator.infrastructure.ledger;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.finledger")
public record FinLedgerProperties(String baseUrl) {

	public FinLedgerProperties {
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = "http://localhost:8080";
		}
	}
}
