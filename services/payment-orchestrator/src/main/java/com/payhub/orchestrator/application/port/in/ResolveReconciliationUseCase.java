package com.payhub.orchestrator.application.port.in;

import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.dto.ResolveReconciliationCommand;

public interface ResolveReconciliationUseCase {

    PaymentView execute(ResolveReconciliationCommand command);
}
