package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

public interface WorkflowPort {

    void startPaymentCapture(PaymentCaptureStart command);

    void signalContinueCapture(UUID paymentId);

    record PaymentCaptureStart(
            UUID paymentId,
            UUID merchantId,
            UUID tenantId,
            String amount,
            String currencyCode,
            String clientReference,
            String sandboxMode
    ) {
    }
}
