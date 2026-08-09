package com.payhub.merchant;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

	@Bean
	@Primary
	JwtDecoder testJwtDecoder() {
		return token -> Jwt.withTokenValue(token)
				.header("alg", "RS256")
				.subject("payhub-test")
				.claim("tenant_id", TestJwtDecoderConfig.TENANT_ID)
				.audience(List.of("payhub"))
				.issuedAt(Instant.now().minusSeconds(60))
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();
	}
}
