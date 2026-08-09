package com.payhub.opsbff.adapter.in.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.opsbff.application.port.in.PaymentOpsUseCase;
import com.payhub.opsbff.application.port.out.OrchestratorPort.PaymentDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/ops/payments")
public class PaymentOpsController {

    private final PaymentOpsUseCase paymentOpsUseCase;

    public PaymentOpsController(PaymentOpsUseCase paymentOpsUseCase) {
        this.paymentOpsUseCase = paymentOpsUseCase;
    }

    @PostMapping("/{id}/refunds")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PaymentDto refund(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RefundRequest request
    ) {
        return paymentOpsUseCase.refund(id, request.amount(), idempotencyKey, request.sandboxMode());
    }

    public record RefundRequest(@NotBlank String amount, String sandboxMode) {
    }
}
