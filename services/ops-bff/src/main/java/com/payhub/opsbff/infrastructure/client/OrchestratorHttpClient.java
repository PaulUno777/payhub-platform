package com.payhub.opsbff.infrastructure.client;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.opsbff.application.port.out.OrchestratorPort;

@Component
public class OrchestratorHttpClient implements OrchestratorPort {

    private final RestClient restClient;

    public OrchestratorHttpClient(
            RestClient.Builder restClientBuilder,
            MerchantServiceProperties properties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(
                        properties.paymentOrchestrator(),
                        "payhub.clients.payment-orchestrator"
                ))
                .build();
    }

    @Override
    public PaymentDto requestRefund(UUID paymentId, String amount, String idempotencyKey, String sandboxMode) {
        return restClient.post()
                .uri("/api/v1/payments/{id}/refunds", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(new RefundBody(amount, sandboxMode))
                .retrieve()
                .body(PaymentDto.class);
    }

    private record RefundBody(String amount, String sandboxMode) {
    }
}
