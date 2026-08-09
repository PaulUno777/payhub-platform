package com.payhub.reporting.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryProjectionJpaRepository extends JpaRepository<JournalEntryProjectionEntity, UUID> {
}
