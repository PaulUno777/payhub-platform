package com.payhub.railadapter.application.port.in;

import java.util.UUID;

public interface SubmitRailOperationUseCase {

    Result execute(Command command);

    record Command(UUID paymentId, String amount, String currencyCode, String sandboxMode) {
    }

    record Result(String outcome, String providerReference) {
    }
}
