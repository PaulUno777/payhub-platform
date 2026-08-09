package com.payhub.orchestrator.application.port.in;

import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.dto.SubmitPaymentCommand;

public interface SubmitPaymentUseCase {

    PaymentView execute(SubmitPaymentCommand command);
}
