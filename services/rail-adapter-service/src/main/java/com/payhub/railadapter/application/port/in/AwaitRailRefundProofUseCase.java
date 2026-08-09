package com.payhub.railadapter.application.port.in;

import java.util.UUID;

public interface AwaitRailRefundProofUseCase {

    Result execute(Command command);

    record Command(UUID paymentId, String providerReference, String sandboxMode) {
    }

    record Result(String outcome) {
    }
}
