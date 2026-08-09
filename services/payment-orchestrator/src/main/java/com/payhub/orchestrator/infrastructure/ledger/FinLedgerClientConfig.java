package com.payhub.orchestrator.infrastructure.ledger;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import io.micrometer.observation.ObservationRegistry;

/**
 * FinLedger / rail / risk RestClient with Micrometer observation (W3C traceparent).
 */
@Configuration
@EnableConfigurationProperties(FinLedgerProperties.class)
public class FinLedgerClientConfig {

    @Bean
    RestClient.Builder payhubRestClientBuilder(ObjectProvider<ObservationRegistry> observationRegistry) {
        RestClient.Builder builder = RestClient.builder();
        observationRegistry.ifAvailable(builder::observationRegistry);
        return builder;
    }
}
