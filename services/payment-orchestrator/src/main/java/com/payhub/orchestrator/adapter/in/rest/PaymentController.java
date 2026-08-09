package com.payhub.orchestrator.adapter.in.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.dto.SubmitPaymentCommand;
import com.payhub.orchestrator.application.port.in.GetPaymentUseCase;
import com.payhub.orchestrator.application.port.in.SubmitPaymentUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final SubmitPaymentUseCase submitPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentController(
            SubmitPaymentUseCase submitPaymentUseCase,
            GetPaymentUseCase getPaymentUseCase
    ) {
        this.submitPaymentUseCase = submitPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SubmitRequest request
    ) {
        PaymentView view = submitPaymentUseCase.execute(new SubmitPaymentCommand(
                request.merchantId(),
                request.tenantId(),
                request.amount(),
                request.currencyCode(),
                request.clientReference(),
                idempotencyKey
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(view));
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentResponse.from(getPaymentUseCase.execute(id));
    }

    public record SubmitRequest(
            @NotNull UUID merchantId,
            @NotNull UUID tenantId,
            @NotBlank String amount,
            @NotBlank String currencyCode,
            @NotBlank String clientReference
    ) {
    }

    public record PaymentResponse(
            UUID id,
            UUID merchantId,
            UUID tenantId,
            String amount,
            String currency,
            String clientReference,
            String status
    ) {
        static PaymentResponse from(PaymentView view) {
            return new PaymentResponse(
                    view.id(),
                    view.merchantId(),
                    view.tenantId(),
                    view.amount(),
                    view.currency(),
                    view.clientReference(),
                    view.status()
            );
        }
    }
}
