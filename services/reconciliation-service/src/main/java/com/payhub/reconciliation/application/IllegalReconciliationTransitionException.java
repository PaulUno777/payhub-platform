package com.payhub.reconciliation.application;

public class IllegalReconciliationTransitionException extends RuntimeException {

    public IllegalReconciliationTransitionException(String message) {
        super(message);
    }

    public IllegalReconciliationTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
