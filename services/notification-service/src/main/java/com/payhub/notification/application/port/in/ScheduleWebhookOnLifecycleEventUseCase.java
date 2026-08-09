package com.payhub.notification.application.port.in;

import com.payhub.notification.application.dto.PaymentLifecycleEnvelope;

public interface ScheduleWebhookOnLifecycleEventUseCase {

    /**
     * @return true when a new {@code WebhookDelivery} was created
     */
    boolean execute(PaymentLifecycleEnvelope envelope);
}
