package com.payhub.reporting.adapter.in.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.payhub.reporting.application.dto.PaymentLifecycleView;
import com.payhub.reporting.application.port.in.GetPaymentLifecycleViewUseCase;

@RestController
@RequestMapping("/api/v1/reporting/payments")
public class PaymentLifecycleViewController {

    public static final String TENANT_HEADER = "X-PayHub-Tenant-Id";

    private final GetPaymentLifecycleViewUseCase getPaymentLifecycleViewUseCase;

    public PaymentLifecycleViewController(GetPaymentLifecycleViewUseCase getPaymentLifecycleViewUseCase) {
        this.getPaymentLifecycleViewUseCase = getPaymentLifecycleViewUseCase;
    }

    @GetMapping("/{paymentId}")
    public PaymentLifecycleView get(
            @PathVariable UUID paymentId,
            @RequestHeader(TENANT_HEADER) UUID tenantId
    ) {
        return getPaymentLifecycleViewUseCase.execute(tenantId, paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment view not found"));
    }
}
