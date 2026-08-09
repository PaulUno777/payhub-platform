package com.payhub.merchant.domain;

public class IllegalMerchantStateException extends RuntimeException {

    public IllegalMerchantStateException(String message) {
        super(message);
    }
}
