package com.payhub.notification.adapter.in.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.payhub.notification.application.port.in.DispatchPendingWebhooksUseCase;

@Component
@ConditionalOnProperty(
        prefix = "payhub.notification.webhooks",
        name = "dispatch-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WebhookDispatchScheduler {

    private final DispatchPendingWebhooksUseCase dispatchPendingWebhooksUseCase;

    public WebhookDispatchScheduler(DispatchPendingWebhooksUseCase dispatchPendingWebhooksUseCase) {
        this.dispatchPendingWebhooksUseCase = dispatchPendingWebhooksUseCase;
    }

    @Scheduled(fixedDelayString = "${payhub.notification.webhooks.dispatch-interval-ms:2000}")
    public void poll() {
        dispatchPendingWebhooksUseCase.execute();
    }
}
