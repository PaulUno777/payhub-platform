package com.payhub.reconciliation.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface OrchestratorPaymentPort {

    Optional<PaymentSnapshot> findById(UUID paymentId);

    PaymentSnapshot resolveReconciliation(UUID paymentId, String action, String idempotencyKey);

    record PaymentSnapshot(UUID id, String status, String clientReference) {
    }
}
