package com.payhub.reporting.application.port.in;

import com.payhub.reporting.application.dto.PaymentLifecycleEnvelope;

public interface ApplyPaymentLifecycleProjectionUseCase {

    /** @return true if applied, false if duplicate inbox hit */
    boolean execute(PaymentLifecycleEnvelope envelope);
}
