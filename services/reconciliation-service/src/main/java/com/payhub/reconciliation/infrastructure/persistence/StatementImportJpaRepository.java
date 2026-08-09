package com.payhub.reconciliation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface StatementImportJpaRepository extends JpaRepository<StatementImportJpaEntity, String> {
}
