package com.payhub.railadapter.adapter.in.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.railadapter.application.port.in.AwaitRailProofUseCase;
import com.payhub.railadapter.application.port.in.AwaitRailRefundProofUseCase;
import com.payhub.railadapter.application.port.in.SubmitRailOperationUseCase;
import com.payhub.railadapter.application.port.in.SubmitRailRefundUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/rail-operations")
public class RailOperationController {

    public static final String SANDBOX_MODE_HEADER = "X-Sandbox-Mode";

    private final SubmitRailOperationUseCase submitRailOperationUseCase;
    private final AwaitRailProofUseCase awaitRailProofUseCase;
    private final SubmitRailRefundUseCase submitRailRefundUseCase;
    private final AwaitRailRefundProofUseCase awaitRailRefundProofUseCase;

    public RailOperationController(
            SubmitRailOperationUseCase submitRailOperationUseCase,
            AwaitRailProofUseCase awaitRailProofUseCase,
            SubmitRailRefundUseCase submitRailRefundUseCase,
            AwaitRailRefundProofUseCase awaitRailRefundProofUseCase
    ) {
        this.submitRailOperationUseCase = submitRailOperationUseCase;
        this.awaitRailProofUseCase = awaitRailProofUseCase;
        this.submitRailRefundUseCase = submitRailRefundUseCase;
        this.awaitRailRefundProofUseCase = awaitRailRefundProofUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public OperationResponse submit(
            @RequestHeader(value = SANDBOX_MODE_HEADER, required = false) String sandboxMode,
            @Valid @RequestBody SubmitRequest request
    ) {
        var result = submitRailOperationUseCase.execute(new SubmitRailOperationUseCase.Command(
                request.paymentId(),
                request.amount(),
                request.currencyCode(),
                sandboxMode
        ));
        return new OperationResponse(result.outcome(), result.providerReference());
    }

    @PostMapping("/{paymentId}/proof")
    @ResponseStatus(HttpStatus.OK)
    public OperationResponse proof(
            @PathVariable UUID paymentId,
            @RequestHeader(value = SANDBOX_MODE_HEADER, required = false) String sandboxMode,
            @Valid @RequestBody ProofRequest request
    ) {
        var result = awaitRailProofUseCase.execute(new AwaitRailProofUseCase.Command(
                paymentId,
                request.providerReference(),
                sandboxMode
        ));
        return new OperationResponse(result.outcome(), request.providerReference());
    }

    @PostMapping("/refunds")
    @ResponseStatus(HttpStatus.OK)
    public OperationResponse submitRefund(
            @RequestHeader(value = SANDBOX_MODE_HEADER, required = false) String sandboxMode,
            @Valid @RequestBody SubmitRequest request
    ) {
        var result = submitRailRefundUseCase.execute(new SubmitRailRefundUseCase.Command(
                request.paymentId(),
                request.amount(),
                request.currencyCode(),
                sandboxMode
        ));
        return new OperationResponse(result.outcome(), result.providerReference());
    }

    @PostMapping("/refunds/{paymentId}/proof")
    @ResponseStatus(HttpStatus.OK)
    public OperationResponse refundProof(
            @PathVariable UUID paymentId,
            @RequestHeader(value = SANDBOX_MODE_HEADER, required = false) String sandboxMode,
            @Valid @RequestBody ProofRequest request
    ) {
        var result = awaitRailRefundProofUseCase.execute(new AwaitRailRefundProofUseCase.Command(
                paymentId,
                request.providerReference(),
                sandboxMode
        ));
        return new OperationResponse(result.outcome(), request.providerReference());
    }

    public record SubmitRequest(
            @NotNull UUID paymentId,
            @NotBlank String amount,
            @NotBlank String currencyCode
    ) {
    }

    public record ProofRequest(@NotBlank String providerReference) {
    }

    public record OperationResponse(String outcome, String providerReference) {
    }
}
