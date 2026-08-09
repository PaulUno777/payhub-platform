package com.payhub.reconciliation.domain;

public class IllegalReconciliationStateException extends RuntimeException {

    public IllegalReconciliationStateException(String message) {
        super(message);
    }
}
