package com.payhub.reporting.application.port.in;

import java.util.Optional;
import java.util.UUID;

import com.payhub.reporting.application.dto.PaymentLifecycleView;

public interface GetPaymentLifecycleViewUseCase {

    Optional<PaymentLifecycleView> execute(UUID tenantId, UUID paymentId);
}
