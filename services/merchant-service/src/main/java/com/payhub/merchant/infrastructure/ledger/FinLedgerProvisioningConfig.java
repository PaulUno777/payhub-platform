package com.payhub.merchant.infrastructure.ledger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FinLedgerProvisioningProperties.class)
public class FinLedgerProvisioningConfig {

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder payhubRestClientBuilder() {
        return RestClient.builder();
    }
}
