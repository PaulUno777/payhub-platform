package com.payhub.reporting.application.usecase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.payhub.reporting.application.dto.PaymentLifecycleView;
import com.payhub.reporting.application.dto.PaymentLifecycleView.Freshness;
import com.payhub.reporting.application.port.in.GetPaymentLifecycleViewUseCase;
import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore;
import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore.PaymentLifecycleProjection;
import com.payhub.reporting.application.port.out.ProjectionCachePort;

@Service
public class GetPaymentLifecycleViewService implements GetPaymentLifecycleViewUseCase {

    private final PaymentLifecycleProjectionStore projectionStore;
    private final ProjectionCachePort projectionCache;
    private final Clock clock;

    public GetPaymentLifecycleViewService(
            PaymentLifecycleProjectionStore projectionStore,
            ProjectionCachePort projectionCache,
            Clock clock
    ) {
        this.projectionStore = projectionStore;
        this.projectionCache = projectionCache;
        this.clock = clock;
    }

    @Override
    public Optional<PaymentLifecycleView> execute(UUID tenantId, UUID paymentId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(paymentId, "paymentId");

        Optional<PaymentLifecycleProjection> cached = projectionCache.getPayment(tenantId, paymentId)
                .filter(p -> tenantId.equals(p.tenantId()));
        if (cached.isPresent()) {
            return Optional.of(toView(cached.get()));
        }

        Optional<PaymentLifecycleProjection> loaded = projectionStore.findByPaymentId(paymentId)
                .filter(p -> tenantId.equals(p.tenantId()));
        loaded.ifPresent(projectionCache::putPayment);
        return loaded.map(this::toView);
    }

    private PaymentLifecycleView toView(PaymentLifecycleProjection projection) {
        Instant now = clock.instant();
        long stalenessMs = Math.max(0, Duration.between(projection.asOf(), now).toMillis());
        return new PaymentLifecycleView(
                projection.paymentId(),
                projection.tenantId(),
                projection.merchantId(),
                projection.status(),
                projection.occurredAt(),
                projection.asOf(),
                stalenessMs,
                Freshness.NON_AUTHORITATIVE
        );
    }
}
