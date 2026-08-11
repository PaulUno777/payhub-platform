package com.payhub.orchestrator.infrastructure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Kind / laptop exit lab only — no Zitadel in-cluster (DS-019). Never enable in compose/prod.
 */
@Configuration
@ConditionalOnProperty(prefix = "payhub.security", name = "lab-open", havingValue = "true")
public class LabOpenSecurityConfig {

    @Bean
    SecurityFilterChain labOpenSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
