package com.payhub.reporting.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore.PaymentLifecycleProjection;

/**
 * Cache-aside for Reporting projections. Infrastructure implements with Redis;
 * application never imports Redis or JSON mapper types.
 */
public interface ProjectionCachePort {

    Optional<PaymentLifecycleProjection> getPayment(UUID tenantId, UUID paymentId);

    void putPayment(PaymentLifecycleProjection projection);

    void evictPayment(UUID tenantId, UUID paymentId);
}
