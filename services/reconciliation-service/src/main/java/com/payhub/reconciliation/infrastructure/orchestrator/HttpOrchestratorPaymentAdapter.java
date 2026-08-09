package com.payhub.reconciliation.infrastructure.orchestrator;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.payhub.reconciliation.application.port.out.OrchestratorPaymentPort;

@Component
public class HttpOrchestratorPaymentAdapter implements OrchestratorPaymentPort {

    private final RestClient restClient;

    public HttpOrchestratorPaymentAdapter(
            RestClient.Builder restClientBuilder,
            OrchestratorClientProperties properties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(
                        properties.paymentOrchestrator(),
                        "payhub.clients.payment-orchestrator"
                ))
                .build();
    }

    @Override
    public Optional<PaymentSnapshot> findById(UUID paymentId) {
        try {
            PaymentResponse body = restClient.get()
                    .uri("/api/v1/payments/{id}", paymentId)
                    .retrieve()
                    .body(PaymentResponse.class);
            if (body == null) {
                return Optional.empty();
            }
            return Optional.of(new PaymentSnapshot(body.id(), body.status(), body.clientReference()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public PaymentSnapshot resolveReconciliation(UUID paymentId, String action, String idempotencyKey) {
        PaymentResponse body = restClient.post()
                .uri("/api/v1/payments/{id}/resolve-reconciliation", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(new ResolveBody(action))
                .retrieve()
                .body(PaymentResponse.class);
        Objects.requireNonNull(body, "orchestrator resolve response");
        return new PaymentSnapshot(body.id(), body.status(), body.clientReference());
    }

    private record PaymentResponse(UUID id, String status, String clientReference) {
    }

    private record ResolveBody(String action) {
    }
}
