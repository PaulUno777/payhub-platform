package com.payhub.merchantbff;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration(proxyBeanMethods = false)
public class TestJwtDecoderConfig {

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("payhub-test")
                .claim("tenant_id", "11111111-1111-1111-1111-111111111111")
                .audience(List.of("payhub"))
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
