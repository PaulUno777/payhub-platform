package com.payhub.orchestrator.application.port.in;

import java.util.UUID;

import com.payhub.orchestrator.application.dto.PaymentView;

public interface GetPaymentUseCase {

    PaymentView execute(UUID paymentId);
}
