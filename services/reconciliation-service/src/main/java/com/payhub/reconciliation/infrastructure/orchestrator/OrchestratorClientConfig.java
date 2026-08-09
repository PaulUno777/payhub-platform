package com.payhub.reconciliation.infrastructure.orchestrator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OrchestratorClientProperties.class)
public class OrchestratorClientConfig {

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder reconciliationRestClientBuilder() {
        return RestClient.builder();
    }
}
