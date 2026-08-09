package com.payhub.opsbff.application.port.in;

import java.util.UUID;

import com.payhub.opsbff.application.port.out.OrchestratorPort.PaymentDto;

public interface PaymentOpsUseCase {

    PaymentDto refund(UUID paymentId, String amount, String idempotencyKey, String sandboxMode);
}
