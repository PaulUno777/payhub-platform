package com.payhub.opsbff.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.payhub.opsbff.application.port.in.PaymentOpsUseCase;
import com.payhub.opsbff.application.port.out.OrchestratorPort;
import com.payhub.opsbff.application.port.out.OrchestratorPort.PaymentDto;

@Service
public class PaymentOpsService implements PaymentOpsUseCase {

    private final OrchestratorPort orchestratorPort;

    public PaymentOpsService(OrchestratorPort orchestratorPort) {
        this.orchestratorPort = orchestratorPort;
    }

    @Override
    public PaymentDto refund(UUID paymentId, String amount, String idempotencyKey, String sandboxMode) {
        return orchestratorPort.requestRefund(paymentId, amount, idempotencyKey, sandboxMode);
    }
}
