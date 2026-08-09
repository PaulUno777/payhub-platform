package com.payhub.orchestrator.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.domain.Payment;

@Component
public class JpaPaymentRepository implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public JpaPaymentRepository(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return PaymentMapper.toDomain(jpaRepository.save(PaymentMapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(PaymentMapper::toDomain);
    }
}
