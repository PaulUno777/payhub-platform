package com.payhub.orchestrator.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.payhub.orchestrator.application.IllegalPaymentTransitionException;
import com.payhub.orchestrator.application.PaymentNotFoundException;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail notFound(PaymentNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setProperty("classification", "terminal");
        return detail;
    }

    @ExceptionHandler(IllegalPaymentTransitionException.class)
    public ProblemDetail conflict(IllegalPaymentTransitionException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setProperty("classification", "terminal");
        return detail;
    }
}
