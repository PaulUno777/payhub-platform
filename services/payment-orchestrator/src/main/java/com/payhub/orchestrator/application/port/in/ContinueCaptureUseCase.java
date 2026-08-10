package com.payhub.orchestrator.application.port.in;

import java.util.UUID;

public interface ContinueCaptureUseCase {

    void execute(UUID paymentId);
}
