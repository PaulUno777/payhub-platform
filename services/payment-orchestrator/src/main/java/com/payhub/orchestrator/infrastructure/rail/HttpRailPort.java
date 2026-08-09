package com.payhub.orchestrator.infrastructure.rail;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.application.port.out.RailPort;

@Component
@ConditionalOnProperty(prefix = "payhub.rail", name = "mode", havingValue = "http")
public class HttpRailPort implements RailPort {

    static final String SANDBOX_MODE_HEADER = "X-Sandbox-Mode";

    private final RestClient restClient;

    public HttpRailPort(
            RestClient.Builder restClientBuilder,
            @Value("${payhub.clients.rail-adapter-service}") String railAdapterBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(railAdapterBaseUrl, "payhub.clients.rail-adapter-service"))
                .build();
    }

    @Override
    public RailSubmitResult submit(RailSubmitCommand command) {
        return postSubmit("/api/v1/rail-operations", command);
    }

    @Override
    public RailProofResult awaitFinalProof(RailProofCommand command) {
        return postProof("/api/v1/rail-operations/{paymentId}/proof", command);
    }

    @Override
    public RailSubmitResult submitRefund(RailSubmitCommand command) {
        return postSubmit("/api/v1/rail-operations/refunds", command);
    }

    @Override
    public RailProofResult awaitRefundProof(RailProofCommand command) {
        return postProof("/api/v1/rail-operations/refunds/{paymentId}/proof", command);
    }

    private RailSubmitResult postSubmit(String path, RailSubmitCommand command) {
        OperationResponse body = restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header(SANDBOX_MODE_HEADER, modeOrDefault(command.sandboxMode()))
                .body(new SubmitRequest(command.paymentId(), command.amount(), command.currencyCode()))
                .retrieve()
                .body(OperationResponse.class);
        if (body == null || body.outcome() == null) {
            throw new IllegalStateException("Empty rail submit response");
        }
        return new RailSubmitResult(RailResult.valueOf(body.outcome()), body.providerReference());
    }

    private RailProofResult postProof(String path, RailProofCommand command) {
        OperationResponse body = restClient.post()
                .uri(path, command.paymentId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(SANDBOX_MODE_HEADER, modeOrDefault(command.sandboxMode()))
                .body(new ProofRequest(command.providerReference()))
                .retrieve()
                .body(OperationResponse.class);
        if (body == null || body.outcome() == null) {
            throw new IllegalStateException("Empty rail proof response");
        }
        return new RailProofResult(RailResult.valueOf(body.outcome()));
    }

    private static String modeOrDefault(String mode) {
        return mode == null || mode.isBlank() ? "ACCEPT" : mode;
    }

    record SubmitRequest(java.util.UUID paymentId, String amount, String currencyCode) {
    }

    record ProofRequest(String providerReference) {
    }

    record OperationResponse(String outcome, String providerReference) {
    }
}
