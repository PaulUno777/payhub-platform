package com.payhub.merchant.application;

public class ProvisioningFailedException extends RuntimeException {

    public ProvisioningFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProvisioningFailedException(String message) {
        super(message);
    }
}
