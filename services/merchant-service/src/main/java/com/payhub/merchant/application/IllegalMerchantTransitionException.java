package com.payhub.merchant.application;

public class IllegalMerchantTransitionException extends RuntimeException {

    public IllegalMerchantTransitionException(String message) {
        super(message);
    }
}
