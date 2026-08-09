package com.payhub.risk.adapter.in.rest;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sandbox risk evaluate — always APPROVED in DS-006 (real rules later).
 */
@RestController
@RequestMapping("/api/v1/risk")
public class RiskEvaluateController {

    @PostMapping("/evaluate")
    public EvaluateResponse evaluate(@Valid @RequestBody EvaluateRequest request) {
        return new EvaluateResponse("APPROVED");
    }

    public record EvaluateRequest(
            @NotNull UUID paymentId,
            @NotNull UUID merchantId,
            @NotNull UUID tenantId,
            @NotBlank String amount,
            @NotBlank String currencyCode
    ) {
    }

    public record EvaluateResponse(String decision) {
    }
}
