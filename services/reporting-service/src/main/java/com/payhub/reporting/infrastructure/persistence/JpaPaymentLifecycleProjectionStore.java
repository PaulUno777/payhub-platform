package com.payhub.reporting.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore;

@Component
public class JpaPaymentLifecycleProjectionStore implements PaymentLifecycleProjectionStore {

    private final PaymentLifecycleProjectionJpaRepository repository;

    public JpaPaymentLifecycleProjectionStore(PaymentLifecycleProjectionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void upsert(PaymentLifecycleProjection projection) {
        PaymentLifecycleProjectionEntity entity = repository.findById(projection.paymentId())
                .orElseGet(() -> new PaymentLifecycleProjectionEntity(
                        projection.paymentId(),
                        projection.tenantId(),
                        projection.merchantId(),
                        projection.eventId(),
                        projection.status(),
                        projection.occurredAt(),
                        projection.asOf()
                ));
        entity.apply(
                projection.tenantId(),
                projection.merchantId(),
                projection.eventId(),
                projection.status(),
                projection.occurredAt(),
                projection.asOf()
        );
        repository.save(entity);
    }

    @Override
    public Optional<PaymentLifecycleProjection> findByPaymentId(UUID paymentId) {
        return repository.findById(paymentId).map(e -> new PaymentLifecycleProjection(
                e.getPaymentId(),
                e.getTenantId(),
                e.getMerchantId(),
                e.getEventId(),
                e.getStatus(),
                e.getOccurredAt(),
                e.getAsOf()
        ));
    }

    @Override
    public long count() {
        return repository.count();
    }
}
