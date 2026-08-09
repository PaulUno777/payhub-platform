package com.payhub.reconciliation.application.port.out;

public interface StatementImportStore {

    boolean alreadyImported(String statementKey);

    void markImported(String statementKey);
}
