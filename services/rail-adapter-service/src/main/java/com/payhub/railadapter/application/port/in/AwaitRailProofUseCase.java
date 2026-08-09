package com.payhub.railadapter.application.port.in;

import java.util.UUID;

public interface AwaitRailProofUseCase {

    Result execute(Command command);

    record Command(UUID paymentId, String providerReference, String sandboxMode) {
    }

    record Result(String outcome) {
    }
}
