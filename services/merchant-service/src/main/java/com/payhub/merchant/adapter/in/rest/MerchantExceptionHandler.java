package com.payhub.merchant.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.payhub.merchant.application.IdempotencyConflictException;
import com.payhub.merchant.application.IllegalMerchantTransitionException;
import com.payhub.merchant.application.MerchantNotFoundException;
import com.payhub.merchant.application.ProvisioningFailedException;

@RestControllerAdvice
public class MerchantExceptionHandler {

    @ExceptionHandler(MerchantNotFoundException.class)
    public ProblemDetail notFound(MerchantNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setProperty("classification", "terminal");
        return detail;
    }

    @ExceptionHandler({IllegalMerchantTransitionException.class, IdempotencyConflictException.class})
    public ProblemDetail conflict(RuntimeException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setProperty("classification", "terminal");
        return detail;
    }

    @ExceptionHandler(ProvisioningFailedException.class)
    public ProblemDetail provisioningFailed(ProvisioningFailedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        detail.setProperty("classification", "retryable");
        return detail;
    }
}
