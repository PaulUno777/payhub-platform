package com.payhub.reporting.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.reporting.application.dto.PaymentLifecycleView;
import com.payhub.reporting.application.dto.PaymentLifecycleView.Freshness;
import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore;
import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore.PaymentLifecycleProjection;
import com.payhub.reporting.application.port.out.ProjectionCachePort;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class GetPaymentLifecycleViewServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");
    private static final Instant AS_OF = Instant.parse("2026-08-09T11:59:00Z");

    @Mock
    private PaymentLifecycleProjectionStore projectionStore;
    @Mock
    private ProjectionCachePort projectionCache;

    private GetPaymentLifecycleViewService service;

    @BeforeEach
    void setUp() {
        service = new GetPaymentLifecycleViewService(
                projectionStore,
                projectionCache,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void should_return_non_authoritative_view_with_staleness_on_cache_miss() {
        UUID tenantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentLifecycleProjection projection = projection(tenantId, paymentId);
        when(projectionCache.getPayment(tenantId, paymentId)).thenReturn(Optional.empty());
        when(projectionStore.findByPaymentId(paymentId)).thenReturn(Optional.of(projection));

        PaymentLifecycleView view = service.execute(tenantId, paymentId).orElseThrow();

        assertThat(view.freshness()).isEqualTo(Freshness.NON_AUTHORITATIVE);
        assertThat(view.asOf()).isEqualTo(AS_OF);
        assertThat(view.stalenessMs()).isEqualTo(60_000L);
        assertThat(view.status()).isEqualTo("SETTLED");
        verify(projectionCache).putPayment(projection);
    }

    @Test
    void should_use_cache_hit_without_store_and_recompute_staleness() {
        UUID tenantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentLifecycleProjection projection = projection(tenantId, paymentId);
        when(projectionCache.getPayment(tenantId, paymentId)).thenReturn(Optional.of(projection));

        PaymentLifecycleView view = service.execute(tenantId, paymentId).orElseThrow();

        assertThat(view.freshness()).isEqualTo(Freshness.NON_AUTHORITATIVE);
        assertThat(view.stalenessMs()).isEqualTo(60_000L);
        verify(projectionStore, never()).findByPaymentId(any());
        verify(projectionCache, never()).putPayment(any());
    }

    @Test
    void should_hide_projection_from_other_tenant() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        when(projectionCache.getPayment(tenantId, paymentId)).thenReturn(Optional.empty());
        when(projectionStore.findByPaymentId(paymentId)).thenReturn(Optional.of(projection(otherTenant, paymentId)));

        assertThat(service.execute(tenantId, paymentId)).isEmpty();
        verify(projectionCache, never()).putPayment(any());
    }

    private static PaymentLifecycleProjection projection(UUID tenantId, UUID paymentId) {
        return new PaymentLifecycleProjection(
                paymentId,
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SETTLED",
                Instant.parse("2026-08-09T11:58:00Z"),
                AS_OF
        );
    }
}
