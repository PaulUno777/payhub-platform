package com.payhub.orchestrator.application;

public class IllegalPaymentTransitionException extends RuntimeException {

    public IllegalPaymentTransitionException(String message) {
        super(message);
    }
}
