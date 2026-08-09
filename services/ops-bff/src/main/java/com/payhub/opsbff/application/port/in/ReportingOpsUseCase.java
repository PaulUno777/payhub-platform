package com.payhub.opsbff.application.port.in;

import java.util.UUID;

import com.payhub.opsbff.application.port.out.ReportingPort.PaymentLifecycleViewDto;

public interface ReportingOpsUseCase {

    PaymentLifecycleViewDto getPaymentLifecycleView(UUID tenantId, UUID paymentId);
}
