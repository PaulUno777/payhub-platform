package com.payhub.reconciliation.application;

public class BreakNotFoundException extends RuntimeException {

    public BreakNotFoundException(java.util.UUID id) {
        super("Break not found: " + id);
    }
}
