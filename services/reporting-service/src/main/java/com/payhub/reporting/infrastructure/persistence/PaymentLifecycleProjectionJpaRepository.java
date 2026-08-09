package com.payhub.reporting.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLifecycleProjectionJpaRepository
        extends JpaRepository<PaymentLifecycleProjectionEntity, UUID> {
}
