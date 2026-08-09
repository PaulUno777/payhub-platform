package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

import com.payhub.orchestrator.domain.PaymentStatus;

public interface PaymentLifecyclePublisher {

    void publishStatusChanged(UUID paymentId, UUID merchantId, UUID tenantId, PaymentStatus status);
}
