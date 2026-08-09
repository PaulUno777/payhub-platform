package com.payhub.notification.infrastructure.config;

import java.time.Clock;
import java.util.HashSet;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import com.payhub.notification.application.WebhookDispatchPolicy;

import io.micrometer.observation.ObservationRegistry;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificationWebhookProperties.class)
public class NotificationInfrastructureConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RestClient.Builder notificationRestClientBuilder(ObjectProvider<ObservationRegistry> observationRegistry) {
        RestClient.Builder builder = RestClient.builder();
        observationRegistry.ifAvailable(builder::observationRegistry);
        return builder;
    }

    @Bean
    WebhookDispatchPolicy webhookDispatchPolicy(NotificationWebhookProperties properties) {
        return new WebhookDispatchPolicy(
                properties.maxAttempts(),
                properties.baseBackoff(),
                properties.dispatchBatchSize(),
                new HashSet<>(properties.notifyStatuses())
        );
    }
}
