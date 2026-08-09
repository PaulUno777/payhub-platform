package com.payhub.opsbff.adapter.in.rest;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.opsbff.application.port.in.ReportingOpsUseCase;
import com.payhub.opsbff.application.port.out.ReportingPort.PaymentLifecycleViewDto;
import com.payhub.opsbff.infrastructure.security.TenantIsolationFilter;

@RestController
@RequestMapping("/ops/reporting")
public class ReportingOpsController {

    private final ReportingOpsUseCase reportingOpsUseCase;

    public ReportingOpsController(ReportingOpsUseCase reportingOpsUseCase) {
        this.reportingOpsUseCase = reportingOpsUseCase;
    }

    @GetMapping("/payments/{paymentId}")
    public PaymentLifecycleViewDto getPaymentLifecycleView(
            @PathVariable UUID paymentId,
            @RequestHeader(TenantIsolationFilter.TENANT_HEADER) UUID tenantId
    ) {
        return reportingOpsUseCase.getPaymentLifecycleView(tenantId, paymentId);
    }
}
