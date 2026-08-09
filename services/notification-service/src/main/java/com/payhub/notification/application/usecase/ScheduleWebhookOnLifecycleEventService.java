package com.payhub.notification.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.messaging.inbox.InboxStore;
import com.payhub.notification.application.WebhookDispatchPolicy;
import com.payhub.notification.application.dto.PaymentLifecycleEnvelope;
import com.payhub.notification.application.port.in.ScheduleWebhookOnLifecycleEventUseCase;
import com.payhub.notification.application.port.out.WebhookDeliveryRepository;
import com.payhub.notification.application.port.out.WebhookEndpointPort;
import com.payhub.notification.domain.WebhookDelivery;

@Service
public class ScheduleWebhookOnLifecycleEventService implements ScheduleWebhookOnLifecycleEventUseCase {

    public static final String CONSUMER = "notification-payment-lifecycle-v1";

    private final InboxStore inboxStore;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEndpointPort endpointPort;
    private final WebhookDispatchPolicy policy;
    private final Clock clock;

    public ScheduleWebhookOnLifecycleEventService(
            InboxStore inboxStore,
            WebhookDeliveryRepository deliveryRepository,
            WebhookEndpointPort endpointPort,
            WebhookDispatchPolicy policy,
            Clock clock
    ) {
        this.inboxStore = inboxStore;
        this.deliveryRepository = deliveryRepository;
        this.endpointPort = endpointPort;
        this.policy = policy;
        this.clock = clock;
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
        String status = payload.status() == null ? "" : payload.status().toUpperCase(Locale.ROOT);
        Instant now = clock.instant();

        if (!policy.shouldNotify(status)) {
            inboxStore.save(envelope.eventId(), CONSUMER);
            return false;
        }

        String endpointUrl = endpointPort.resolveUrl(payload.tenantId()).orElse("");
        WebhookDelivery delivery = WebhookDelivery.schedule(
                envelope.eventId(),
                payload.paymentId(),
                payload.tenantId(),
                status,
                envelope.occurredAt() != null ? envelope.occurredAt() : now,
                endpointUrl,
                now
        );
        deliveryRepository.save(delivery);
        inboxStore.save(envelope.eventId(), CONSUMER);
        return true;
    }
}
