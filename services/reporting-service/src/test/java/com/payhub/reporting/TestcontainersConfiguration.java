package com.payhub.reporting;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

	@Bean
	RedisContainer redisContainer() {
		return new RedisContainer(DockerImageName.parse("redis:7.4-alpine"));
	}

	@Bean
	DynamicPropertyRegistrar redisProperties(RedisContainer redisContainer) {
		return registry -> {
			registry.add("spring.data.redis.host", redisContainer::getRedisHost);
			registry.add("spring.data.redis.port", () -> String.valueOf(redisContainer.getRedisPort()));
		};
	}

}
