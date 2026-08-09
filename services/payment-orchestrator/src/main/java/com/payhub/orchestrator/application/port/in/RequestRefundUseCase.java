package com.payhub.orchestrator.application.port.in;

import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.dto.RequestRefundCommand;

public interface RequestRefundUseCase {

    PaymentView execute(RequestRefundCommand command);
}
