package com.payhub.orchestrator.infrastructure.risk;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.application.port.out.RiskPort;

@Component
@ConditionalOnProperty(prefix = "payhub.risk", name = "mode", havingValue = "http")
public class HttpRiskAdapter implements RiskPort {

    private final RestClient restClient;

    public HttpRiskAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${payhub.clients.risk-service}") String riskServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(riskServiceBaseUrl, "payhub.clients.risk-service"))
                .build();
    }

    @Override
    public RiskDecision evaluate(RiskCommand command) {
        RiskResponse body = restClient.post()
                .uri("/api/v1/risk/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RiskRequest(
                        command.paymentId(),
                        command.merchantId(),
                        command.tenantId(),
                        command.amount(),
                        command.currencyCode()
                ))
                .retrieve()
                .body(RiskResponse.class);
        if (body == null || body.decision() == null) {
            throw new IllegalStateException("Empty risk evaluate response");
        }
        return RiskDecision.valueOf(body.decision());
    }

    record RiskRequest(
            java.util.UUID paymentId,
            java.util.UUID merchantId,
            java.util.UUID tenantId,
            String amount,
            String currencyCode
    ) {
    }

    record RiskResponse(String decision) {
    }
}
