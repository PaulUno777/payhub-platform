package com.payhub.opsbff.infrastructure.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import io.micrometer.observation.ObservationRegistry;

/**
 * Downstream HTTP clients — RestClient with Micrometer observation (W3C traceparent).
 */
@Configuration
@EnableConfigurationProperties(MerchantServiceProperties.class)
public class MerchantServiceClientConfig {

    @Bean
    RestClient.Builder opsBffRestClientBuilder(ObjectProvider<ObservationRegistry> observationRegistry) {
        RestClient.Builder builder = RestClient.builder();
        observationRegistry.ifAvailable(builder::observationRegistry);
        return builder;
    }
}
