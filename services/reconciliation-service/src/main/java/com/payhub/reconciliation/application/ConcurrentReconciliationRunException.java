package com.payhub.reconciliation.application;

public class ConcurrentReconciliationRunException extends RuntimeException {

    public ConcurrentReconciliationRunException(String message) {
        super(message);
    }
}
