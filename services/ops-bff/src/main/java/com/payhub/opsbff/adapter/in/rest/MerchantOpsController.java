package com.payhub.opsbff.adapter.in.rest;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.opsbff.application.port.in.MerchantOpsUseCase;
import com.payhub.opsbff.application.port.out.MerchantServicePort.MerchantDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantOpsController {

    private final MerchantOpsUseCase merchantOpsUseCase;

    public MerchantOpsController(MerchantOpsUseCase merchantOpsUseCase) {
        this.merchantOpsUseCase = merchantOpsUseCase;
    }

    @GetMapping("/{id}")
    public MerchantDto get(@PathVariable UUID id) {
        return merchantOpsUseCase.get(id);
    }

    @PostMapping("/{id}/approve")
    public MerchantDto approve(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return merchantOpsUseCase.approve(id, idempotencyKey, authorization);
    }

    @PostMapping("/{id}/reject")
    public MerchantDto reject(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RejectRequest request
    ) {
        return merchantOpsUseCase.reject(id, request.reason(), idempotencyKey);
    }

    public record RejectRequest(@NotBlank String reason) {
    }
}
