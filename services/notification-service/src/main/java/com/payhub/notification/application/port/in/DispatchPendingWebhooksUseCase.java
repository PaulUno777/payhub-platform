package com.payhub.notification.application.port.in;

public interface DispatchPendingWebhooksUseCase {

    /**
     * @return number of deliveries attempted in this poll
     */
    int execute();
}
