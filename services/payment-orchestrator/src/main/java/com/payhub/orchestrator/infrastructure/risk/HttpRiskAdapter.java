package com.payhub.orchestrator.infrastructure.risk;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.application.RetryableDependencyException;
import com.payhub.orchestrator.application.port.out.RiskPort;
import com.payhub.orchestrator.infrastructure.resilience.OutboundResilienceConfig;
import com.payhub.orchestrator.infrastructure.resilience.OutboundResilienceConfig.DependencyResilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

@Component
@ConditionalOnProperty(prefix = "payhub.risk", name = "mode", havingValue = "http")
public class HttpRiskAdapter implements RiskPort {

    private static final String DEPENDENCY = "risk";

    private final RestClient restClient;
    private final DependencyResilience resilience;

    public HttpRiskAdapter(
            @Qualifier(OutboundResilienceConfig.RISK_CLIENT) RestClient restClient,
            DependencyResilience resilience
    ) {
        this.restClient = Objects.requireNonNull(restClient, "riskRestClient");
        this.resilience = Objects.requireNonNull(resilience, "resilience");
    }

    @Override
    public RiskDecision evaluate(RiskCommand command) {
        try {
            return resilience.execute(DEPENDENCY, () -> {
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
            });
        } catch (BulkheadFullException | CallNotPermittedException ex) {
            throw new RetryableDependencyException(DEPENDENCY, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            if (isTimeout(ex)) {
                throw new RetryableDependencyException(DEPENDENCY, "Risk call timed out", ex);
            }
            throw ex;
        }
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof TimeoutException) {
                return true;
            }
            if (cur.getClass().getName().contains("Timeout")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
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
