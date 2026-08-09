package com.payhub.reconciliation.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.payhub.reconciliation.application.BreakNotFoundException;
import com.payhub.reconciliation.application.ConcurrentReconciliationRunException;
import com.payhub.reconciliation.application.IllegalReconciliationTransitionException;

@RestControllerAdvice
public class ReconciliationExceptionHandler {

    @ExceptionHandler(BreakNotFoundException.class)
    ProblemDetail notFound(BreakNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConcurrentReconciliationRunException.class)
    ProblemDetail conflict(ConcurrentReconciliationRunException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({IllegalReconciliationTransitionException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
