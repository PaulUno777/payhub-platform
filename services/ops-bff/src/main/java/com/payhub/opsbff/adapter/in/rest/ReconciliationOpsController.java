package com.payhub.opsbff.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.opsbff.application.port.in.ReconciliationOpsUseCase;
import com.payhub.opsbff.application.port.out.ReconciliationPort.BreakDto;
import com.payhub.opsbff.application.port.out.ReconciliationPort.RunDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/ops/reconciliation")
public class ReconciliationOpsController {

    private final ReconciliationOpsUseCase reconciliationOpsUseCase;

    public ReconciliationOpsController(ReconciliationOpsUseCase reconciliationOpsUseCase) {
        this.reconciliationOpsUseCase = reconciliationOpsUseCase;
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public RunDto startRun(@Valid @RequestBody StartRunRequest request) {
        return reconciliationOpsUseCase.startRun(
                request.tenantId(),
                request.railCode(),
                request.statementKey()
        );
    }

    @GetMapping("/breaks")
    public List<BreakDto> listBreaks(@RequestParam(required = false) UUID runId) {
        return reconciliationOpsUseCase.listBreaks(runId);
    }

    @GetMapping("/breaks/{id}")
    public BreakDto getBreak(@PathVariable UUID id) {
        return reconciliationOpsUseCase.getBreak(id);
    }

    @PostMapping("/breaks/{id}/resolve")
    public BreakDto resolve(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ResolveRequest request
    ) {
        return reconciliationOpsUseCase.resolve(id, request.action(), request.actor(), idempotencyKey);
    }

    public record StartRunRequest(
            @NotNull UUID tenantId,
            @NotBlank String railCode,
            @NotBlank String statementKey
    ) {
    }

    public record ResolveRequest(
            @NotBlank String action,
            @NotBlank String actor
    ) {
    }
}
