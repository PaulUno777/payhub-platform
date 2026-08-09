package com.payhub.orchestrator.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.payhub.orchestrator.domain.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);
}
