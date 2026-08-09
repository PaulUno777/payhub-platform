package com.payhub.orchestrator.infrastructure.ledger;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FinLedgerProperties.class)
public class FinLedgerClientConfig {
}
