package com.payhub.reconciliation.adapter.in.rest;

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

import com.payhub.reconciliation.application.dto.BreakView;
import com.payhub.reconciliation.application.dto.ReconciliationRunView;
import com.payhub.reconciliation.application.port.in.GetBreakUseCase;
import com.payhub.reconciliation.application.port.in.ListBreaksUseCase;
import com.payhub.reconciliation.application.port.in.ResolveBreakUseCase;
import com.payhub.reconciliation.application.port.in.StartReconciliationRunUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final StartReconciliationRunUseCase startRunUseCase;
    private final ListBreaksUseCase listBreaksUseCase;
    private final GetBreakUseCase getBreakUseCase;
    private final ResolveBreakUseCase resolveBreakUseCase;

    public ReconciliationController(
            StartReconciliationRunUseCase startRunUseCase,
            ListBreaksUseCase listBreaksUseCase,
            GetBreakUseCase getBreakUseCase,
            ResolveBreakUseCase resolveBreakUseCase
    ) {
        this.startRunUseCase = startRunUseCase;
        this.listBreaksUseCase = listBreaksUseCase;
        this.getBreakUseCase = getBreakUseCase;
        this.resolveBreakUseCase = resolveBreakUseCase;
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ReconciliationRunView startRun(@Valid @RequestBody StartRunRequest request) {
        return startRunUseCase.execute(new StartReconciliationRunUseCase.Command(
                request.tenantId(),
                request.railCode(),
                request.statementKey()
        ));
    }

    @GetMapping("/breaks")
    public List<BreakView> listBreaks(@RequestParam(required = false) UUID runId) {
        if (runId != null) {
            return listBreaksUseCase.byRun(runId);
        }
        return listBreaksUseCase.openBreaks();
    }

    @GetMapping("/breaks/{id}")
    public BreakView getBreak(@PathVariable UUID id) {
        return getBreakUseCase.execute(id);
    }

    @PostMapping("/breaks/{id}/resolve")
    public BreakView resolve(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ResolveRequest request
    ) {
        return resolveBreakUseCase.execute(new ResolveBreakUseCase.Command(
                id,
                request.action(),
                request.actor(),
                idempotencyKey
        ));
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
