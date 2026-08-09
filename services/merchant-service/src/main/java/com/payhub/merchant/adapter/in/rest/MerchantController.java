package com.payhub.merchant.adapter.in.rest;

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

import com.payhub.merchant.application.dto.ApproveMerchantCommand;
import com.payhub.merchant.application.dto.MerchantView;
import com.payhub.merchant.application.dto.RegisterMerchantCommand;
import com.payhub.merchant.application.dto.RejectMerchantCommand;
import com.payhub.merchant.application.port.in.ApproveMerchantUseCase;
import com.payhub.merchant.application.port.in.GetMerchantUseCase;
import com.payhub.merchant.application.port.in.RegisterMerchantUseCase;
import com.payhub.merchant.application.port.in.RejectMerchantUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final RegisterMerchantUseCase registerMerchantUseCase;
    private final ApproveMerchantUseCase approveMerchantUseCase;
    private final RejectMerchantUseCase rejectMerchantUseCase;
    private final GetMerchantUseCase getMerchantUseCase;

    public MerchantController(
            RegisterMerchantUseCase registerMerchantUseCase,
            ApproveMerchantUseCase approveMerchantUseCase,
            RejectMerchantUseCase rejectMerchantUseCase,
            GetMerchantUseCase getMerchantUseCase
    ) {
        this.registerMerchantUseCase = registerMerchantUseCase;
        this.approveMerchantUseCase = approveMerchantUseCase;
        this.rejectMerchantUseCase = rejectMerchantUseCase;
        this.getMerchantUseCase = getMerchantUseCase;
    }

    @PostMapping
    public ResponseEntity<MerchantResponse> register(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RegisterRequest request
    ) {
        MerchantView view = registerMerchantUseCase.execute(new RegisterMerchantCommand(
                request.legalName(),
                request.tier(),
                request.assignedRuleSetKey(),
                idempotencyKey
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(MerchantResponse.from(view));
    }

    @GetMapping("/{id}")
    public MerchantResponse get(@PathVariable UUID id) {
        return MerchantResponse.from(getMerchantUseCase.execute(id));
    }

    @PostMapping("/{id}/approve")
    public MerchantResponse approve(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String bearer = extractBearer(authorization);
        MerchantView view = approveMerchantUseCase.execute(
                new ApproveMerchantCommand(id, idempotencyKey, bearer)
        );
        return MerchantResponse.from(view);
    }

    @PostMapping("/{id}/reject")
    public MerchantResponse reject(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RejectRequest request
    ) {
        MerchantView view = rejectMerchantUseCase.execute(
                new RejectMerchantCommand(id, request.reason(), idempotencyKey)
        );
        return MerchantResponse.from(view);
    }

    private static String extractBearer(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }

    public record RegisterRequest(
            @NotBlank String legalName,
            @NotBlank String tier,
            @NotBlank String assignedRuleSetKey
    ) {
    }

    public record RejectRequest(@NotBlank String reason) {
    }

    public record MerchantResponse(
            UUID id,
            String legalName,
            String status,
            String tier,
            String assignedRuleSetKey,
            UUID finLedgerTenantId,
            UUID merchantWalletId,
            UUID settlementWalletId,
            String rejectionReason
    ) {
        static MerchantResponse from(MerchantView view) {
            return new MerchantResponse(
                    view.id(),
                    view.legalName(),
                    view.status(),
                    view.tier(),
                    view.assignedRuleSetKey(),
                    view.finLedgerTenantId(),
                    view.merchantWalletId(),
                    view.settlementWalletId(),
                    view.rejectionReason()
            );
        }
    }
}
