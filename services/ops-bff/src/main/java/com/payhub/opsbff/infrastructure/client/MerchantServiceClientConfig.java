package com.payhub.opsbff.infrastructure.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MerchantServiceProperties.class)
public class MerchantServiceClientConfig {

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder opsBffRestClientBuilder() {
        return RestClient.builder();
    }
}
