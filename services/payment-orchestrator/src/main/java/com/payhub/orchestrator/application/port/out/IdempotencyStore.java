package com.payhub.orchestrator.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {

    Optional<UUID> findPaymentId(String operation, String idempotencyKey);

    void save(String operation, String idempotencyKey, String requestHash, UUID paymentId);
}
