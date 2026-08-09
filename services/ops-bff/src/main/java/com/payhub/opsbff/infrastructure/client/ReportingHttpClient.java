package com.payhub.opsbff.infrastructure.client;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.opsbff.application.port.out.ReportingPort;
import com.payhub.opsbff.infrastructure.security.TenantIsolationFilter;

@Component
public class ReportingHttpClient implements ReportingPort {

    private final RestClient restClient;

    public ReportingHttpClient(
            RestClient.Builder restClientBuilder,
            MerchantServiceProperties properties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(
                        properties.reportingService(),
                        "payhub.clients.reporting-service"
                ))
                .build();
    }

    @Override
    public PaymentLifecycleViewDto getPaymentLifecycleView(UUID tenantId, UUID paymentId) {
        return restClient.get()
                .uri("/api/v1/reporting/payments/{id}", paymentId)
                .header(TenantIsolationFilter.TENANT_HEADER, tenantId.toString())
                .retrieve()
                .body(PaymentLifecycleViewDto.class);
    }
}
