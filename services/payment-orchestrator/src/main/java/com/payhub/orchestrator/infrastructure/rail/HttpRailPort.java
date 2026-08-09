package com.payhub.orchestrator.infrastructure.rail;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.application.port.out.RailPort;
import com.payhub.orchestrator.infrastructure.resilience.OutboundResilienceConfig;
import com.payhub.orchestrator.infrastructure.resilience.OutboundResilienceConfig.DependencyResilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

@Component
@ConditionalOnProperty(prefix = "payhub.rail", name = "mode", havingValue = "http")
public class HttpRailPort implements RailPort {

    static final String SANDBOX_MODE_HEADER = "X-Sandbox-Mode";
    private static final String DEPENDENCY = "rail";

    private final RestClient restClient;
    private final DependencyResilience resilience;

    public HttpRailPort(
            @Qualifier(OutboundResilienceConfig.RAIL_CLIENT) RestClient restClient,
            DependencyResilience resilience
    ) {
        this.restClient = Objects.requireNonNull(restClient, "railRestClient");
        this.resilience = Objects.requireNonNull(resilience, "resilience");
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
        try {
            return resilience.execute(DEPENDENCY, () -> {
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
            });
        } catch (BulkheadFullException | CallNotPermittedException ex) {
            return new RailSubmitResult(RailResult.AMBIGUOUS, null);
        } catch (RuntimeException ex) {
            if (isTimeoutOrTransportAmbiguity(ex)) {
                return new RailSubmitResult(RailResult.AMBIGUOUS, null);
            }
            throw ex;
        }
    }

    private RailProofResult postProof(String path, RailProofCommand command) {
        try {
            return resilience.execute(DEPENDENCY, () -> {
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
            });
        } catch (BulkheadFullException | CallNotPermittedException ex) {
            return new RailProofResult(RailResult.AMBIGUOUS);
        } catch (RuntimeException ex) {
            if (isTimeoutOrTransportAmbiguity(ex)) {
                return new RailProofResult(RailResult.AMBIGUOUS);
            }
            throw ex;
        }
    }

    private static boolean isTimeoutOrTransportAmbiguity(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof TimeoutException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Timeout") || name.contains("ResourceAccess")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
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
