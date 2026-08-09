package com.payhub.notification.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.notification.application.WebhookDispatchPolicy;
import com.payhub.notification.application.port.in.DispatchPendingWebhooksUseCase;
import com.payhub.notification.application.port.out.WebhookDeliveryRepository;
import com.payhub.notification.application.port.out.WebhookTransportPort;
import com.payhub.notification.application.port.out.WebhookTransportPort.DeliveryResult;
import com.payhub.notification.application.port.out.WebhookTransportPort.WebhookPayload;
import com.payhub.notification.domain.WebhookDelivery;
import com.payhub.notification.domain.WebhookDeliveryStatus;

@Service
public class DispatchPendingWebhooksService implements DispatchPendingWebhooksUseCase {

    private static final Logger log = LoggerFactory.getLogger(DispatchPendingWebhooksService.class);

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookTransportPort transportPort;
    private final WebhookDispatchPolicy policy;
    private final Clock clock;

    public DispatchPendingWebhooksService(
            WebhookDeliveryRepository deliveryRepository,
            WebhookTransportPort transportPort,
            WebhookDispatchPolicy policy,
            Clock clock
    ) {
        this.deliveryRepository = deliveryRepository;
        this.transportPort = transportPort;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int execute() {
        Instant now = clock.instant();
        List<WebhookDelivery> due = deliveryRepository.findDue(now, policy.dispatchBatchSize());
        for (WebhookDelivery delivery : due) {
            delivery.claim(now);
            deliveryRepository.save(delivery);

            DeliveryResult result = transportPort.deliver(
                    new WebhookPayload(
                            delivery.eventId(),
                            delivery.paymentId(),
                            delivery.tenantId(),
                            delivery.paymentStatus(),
                            delivery.occurredAt()
                    ),
                    delivery.endpointUrl()
            );

            Instant after = clock.instant();
            if (result.success()) {
                delivery.markDelivered(after);
            } else {
                delivery.recordFailure(
                        result.errorMessage(),
                        policy.maxAttempts(),
                        policy.baseBackoff(),
                        after
                );
                if (delivery.status() == WebhookDeliveryStatus.DEAD) {
                    log.warn(
                            "Webhook delivery exhausted attempts deliveryId={} paymentId={} lastError={}",
                            delivery.id(),
                            delivery.paymentId(),
                            delivery.lastError()
                    );
                }
            }
            deliveryRepository.save(delivery);
        }
        return due.size();
    }
}
