package com.payhub.reconciliation.infrastructure.persistence;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.payhub.reconciliation.application.port.out.StatementImportStore;

@Component
public class JpaStatementImportStore implements StatementImportStore {

    private final StatementImportJpaRepository jpaRepository;

    public JpaStatementImportStore(StatementImportJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean alreadyImported(String statementKey) {
        return jpaRepository.existsById(statementKey);
    }

    @Override
    public void markImported(String statementKey) {
        jpaRepository.save(new StatementImportJpaEntity(statementKey, Instant.now()));
    }
}
