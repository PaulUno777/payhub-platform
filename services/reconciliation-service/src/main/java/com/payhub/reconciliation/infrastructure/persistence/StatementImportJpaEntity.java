package com.payhub.reconciliation.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "statement_import")
public class StatementImportJpaEntity {

    @Id
    @Column(name = "statement_key", length = 128)
    private String statementKey;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected StatementImportJpaEntity() {
    }

    public StatementImportJpaEntity(String statementKey, Instant importedAt) {
        this.statementKey = statementKey;
        this.importedAt = importedAt;
    }

    public String getStatementKey() { return statementKey; }
    public Instant getImportedAt() { return importedAt; }
}
