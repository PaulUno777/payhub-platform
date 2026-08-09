package com.payhub.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.messaging.inbox.InboxStore;
import com.payhub.notification.application.WebhookDispatchPolicy;
import com.payhub.notification.application.dto.PaymentLifecycleEnvelope;
import com.payhub.notification.application.dto.PaymentLifecycleEnvelope.PaymentStatusChangedPayload;
import com.payhub.notification.application.port.out.WebhookDeliveryRepository;
import com.payhub.notification.application.port.out.WebhookEndpointPort;
import com.payhub.notification.application.port.out.WebhookTransportPort;
import com.payhub.notification.application.port.out.WebhookTransportPort.DeliveryResult;
import com.payhub.notification.domain.WebhookDelivery;
import com.payhub.notification.domain.WebhookDeliveryStatus;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class WebhookUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");

    @Mock
    private InboxStore inboxStore;
    @Mock
    private WebhookDeliveryRepository deliveryRepository;
    @Mock
    private WebhookEndpointPort endpointPort;
    @Mock
    private WebhookTransportPort transportPort;

    private WebhookDispatchPolicy policy;
    private Clock clock;
    private ScheduleWebhookOnLifecycleEventService schedule;
    private DispatchPendingWebhooksService dispatch;

    @BeforeEach
    void setUp() {
        policy = new WebhookDispatchPolicy(2, Duration.ofSeconds(1), 10, Set.of("SETTLED", "FAILED_FINAL"));
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        schedule = new ScheduleWebhookOnLifecycleEventService(
                inboxStore, deliveryRepository, endpointPort, policy, clock
        );
        dispatch = new DispatchPendingWebhooksService(deliveryRepository, transportPort, policy, clock);
    }

    @Test
    void should_schedule_settled_and_skip_duplicate() {
        when(inboxStore.exists(any())).thenReturn(false);
        when(endpointPort.resolveUrl(any())).thenReturn(Optional.of("https://hooks.example/pay"));

        PaymentLifecycleEnvelope envelope = sample("SETTLED");
        assertThat(schedule.execute(envelope)).isTrue();
        verify(deliveryRepository).save(any(WebhookDelivery.class));
        verify(inboxStore).save(eq(envelope.eventId()), eq(ScheduleWebhookOnLifecycleEventService.CONSUMER));

        when(inboxStore.exists(envelope.eventId())).thenReturn(true);
        assertThat(schedule.execute(envelope)).isFalse();
        verify(deliveryRepository).save(any(WebhookDelivery.class));
    }

    @Test
    void should_ack_inbox_without_delivery_for_noise_status() {
        when(inboxStore.exists(any())).thenReturn(false);
        PaymentLifecycleEnvelope envelope = sample("RISK_PENDING");
        assertThat(schedule.execute(envelope)).isFalse();
        verify(deliveryRepository, never()).save(any());
        verify(inboxStore).save(eq(envelope.eventId()), eq(ScheduleWebhookOnLifecycleEventService.CONSUMER));
    }

    @Test
    void should_move_to_dead_after_exhausted_retries() {
        WebhookDelivery delivery = WebhookDelivery.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SETTLED",
                NOW,
                "https://hooks.example/pay",
                NOW
        );
        when(deliveryRepository.findDue(eq(NOW), anyInt())).thenReturn(List.of(delivery));
        when(transportPort.deliver(any(), any())).thenReturn(DeliveryResult.failed(500, "HTTP 500"));

        assertThat(dispatch.execute()).isEqualTo(1);
        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        WebhookDelivery last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(last.status()).isEqualTo(WebhookDeliveryStatus.FAILED_RETRYABLE);
        assertThat(last.attemptCount()).isEqualTo(1);

        when(deliveryRepository.findDue(eq(NOW), anyInt())).thenReturn(List.of(last));
        assertThat(dispatch.execute()).isEqualTo(1);
        verify(deliveryRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());
        WebhookDelivery dead = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(dead.status()).isEqualTo(WebhookDeliveryStatus.DEAD);
        assertThat(dead.attemptCount()).isEqualTo(2);
    }

    private static PaymentLifecycleEnvelope sample(String status) {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        return new PaymentLifecycleEnvelope(
                eventId,
                "PaymentStatusChanged",
                "1",
                NOW,
                "payment-orchestrator",
                paymentId,
                tenantId,
                null,
                null,
                new PaymentStatusChangedPayload(paymentId, UUID.randomUUID(), tenantId, status)
        );
    }
}
