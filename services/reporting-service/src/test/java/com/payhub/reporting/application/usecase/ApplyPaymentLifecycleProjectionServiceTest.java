package com.payhub.reporting.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.messaging.inbox.InboxStore;
import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope;
import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope.PaymentStatusChangedPayload;
import com.payhub.reporting.application.port.out.PaymentLifecycleProjectionStore;
import com.payhub.reporting.application.port.out.ProjectionCachePort;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ApplyPaymentLifecycleProjectionServiceTest {

    @Mock
    private InboxStore inboxStore;
    @Mock
    private PaymentLifecycleProjectionStore projectionStore;
    @Mock
    private ProjectionCachePort projectionCache;

    private ApplyPaymentLifecycleProjectionService service;

    @BeforeEach
    void setUp() {
        service = new ApplyPaymentLifecycleProjectionService(inboxStore, projectionStore, projectionCache);
    }

    @Test
    void should_apply_once_and_skip_duplicate_event_id() {
        PaymentLifecycleEnvelope envelope = sample();
        when(inboxStore.exists(envelope.eventId())).thenReturn(false, true);

        assertThat(service.execute(envelope)).isTrue();
        assertThat(service.execute(envelope)).isFalse();

        verify(projectionStore).upsert(any());
        verify(inboxStore).save(envelope.eventId(), ApplyPaymentLifecycleProjectionService.CONSUMER);
        verify(projectionCache).evictPayment(envelope.payload().tenantId(), envelope.payload().paymentId());
    }

    @Test
    void should_noop_when_inbox_already_has_event() {
        PaymentLifecycleEnvelope envelope = sample();
        when(inboxStore.exists(envelope.eventId())).thenReturn(true);

        assertThat(service.execute(envelope)).isFalse();
        verify(projectionStore, never()).upsert(any());
        verify(inboxStore, never()).save(any(), any());
        verify(projectionCache, never()).evictPayment(any(), any());
    }

    private static PaymentLifecycleEnvelope sample() {
        UUID paymentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        return new PaymentLifecycleEnvelope(
                UUID.randomUUID(),
                "PaymentStatusChanged",
                "1",
                Instant.parse("2026-08-09T05:00:00Z"),
                "payment-orchestrator",
                paymentId,
                tenantId,
                null,
                null,
                new PaymentStatusChangedPayload(paymentId, UUID.randomUUID(), tenantId, "RISK_APPROVED")
        );
    }
}
