package com.payhub.reporting.application.usecase;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.messaging.inbox.InboxStore;
import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope;
import com.payhub.reporting.application.port.in.ApplyPaymentLifecycleProjectionUseCase;
import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore;
import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore.PaymentLifecycleProjection;
import com.payhub.reporting.application.port.out.ProjectionCachePort;

@Service
public class ApplyPaymentLifecycleProjectionService implements ApplyPaymentLifecycleProjectionUseCase {

    public static final String CONSUMER = "reporting-payment-lifecycle-v1";

    private final InboxStore inboxStore;
    private final PaymentLifecycleProjectionStore projectionStore;
    private final ProjectionCachePort projectionCache;

    public ApplyPaymentLifecycleProjectionService(
            InboxStore inboxStore,
            PaymentLifecycleProjectionStore projectionStore,
            ProjectionCachePort projectionCache
    ) {
        this.inboxStore = inboxStore;
        this.projectionStore = projectionStore;
        this.projectionCache = projectionCache;
    }

    @Override
    @Transactional
    public boolean execute(PaymentLifecycleEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(envelope.eventId(), "eventId");
        Objects.requireNonNull(envelope.payload(), "payload");

        if (inboxStore.exists(envelope.eventId())) {
            return false;
        }

        var payload = envelope.payload();
        Instant asOf = Instant.now();
        projectionStore.upsert(new PaymentLifecycleProjection(
                payload.paymentId(),
                payload.tenantId(),
                payload.merchantId(),
                envelope.eventId(),
                payload.status(),
                envelope.occurredAt(),
                asOf
        ));
        inboxStore.save(envelope.eventId(), CONSUMER);
        projectionCache.evictPayment(payload.tenantId(), payload.paymentId());
        return true;
    }
}
