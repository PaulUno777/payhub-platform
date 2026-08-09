package com.payhub.reporting.infrastructure.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore.PaymentLifecycleProjection;
import com.payhub.reporting.application.port.out.ProjectionCachePort;

import tools.jackson.databind.ObjectMapper;

@Component
public class RedisProjectionCacheAdapter implements ProjectionCachePort {

    public static final String KEY_PREFIX = "reporting:payment:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisProjectionCacheAdapter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${payhub.reporting.cache.ttl:PT30S}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    static String key(UUID tenantId, UUID paymentId) {
        return KEY_PREFIX + tenantId + ":" + paymentId;
    }

    @Override
    public Optional<PaymentLifecycleProjection> getPayment(UUID tenantId, UUID paymentId) {
        String json = redisTemplate.opsForValue().get(key(tenantId, paymentId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, PaymentLifecycleProjection.class));
        }
        catch (Exception ex) {
            redisTemplate.delete(key(tenantId, paymentId));
            return Optional.empty();
        }
    }

    @Override
    public void putPayment(PaymentLifecycleProjection projection) {
        try {
            String json = objectMapper.writeValueAsString(projection);
            redisTemplate.opsForValue().set(key(projection.tenantId(), projection.paymentId()), json, ttl);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to cache payment projection", ex);
        }
    }

    @Override
    public void evictPayment(UUID tenantId, UUID paymentId) {
        redisTemplate.delete(key(tenantId, paymentId));
    }
}
