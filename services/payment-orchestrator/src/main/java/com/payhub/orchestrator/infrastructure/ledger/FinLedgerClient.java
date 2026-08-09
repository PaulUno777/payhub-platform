package com.payhub.orchestrator.infrastructure.ledger;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.application.RetryableDependencyException;
import com.payhub.orchestrator.application.port.out.LedgerPort;
import com.payhub.orchestrator.infrastructure.resilience.OutboundResilienceConfig;
import com.payhub.orchestrator.infrastructure.resilience.OutboundResilienceConfig.DependencyResilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

@Component
public class FinLedgerClient implements LedgerPort {

    private static final String DEPENDENCY = "finledger";

    private final RestClient restClient;
    private final DependencyResilience resilience;

    public FinLedgerClient(
            @Qualifier(OutboundResilienceConfig.FIN_LEDGER_CLIENT) RestClient restClient,
            DependencyResilience resilience
    ) {
        this.restClient = Objects.requireNonNull(restClient, "finLedgerRestClient");
        this.resilience = Objects.requireNonNull(resilience, "resilience");
    }

    @Override
    public InitiateRailPaymentResult initiateRailPayment(InitiateRailPaymentCommand command) {
        return execute(() -> {
            FinLedgerInitiateResponse body = restClient.post()
                    .uri("/api/v1/tenants/{tenantId}/rails/payments", command.tenantId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", command.idempotencyKey())
                    .header("Authorization", "Bearer " + command.bearerToken())
                    .body(new FinLedgerInitiateRequest(
                            command.railCode(),
                            command.amount(),
                            command.currencyCode(),
                            command.clearingAccountId(),
                            command.counterpartyAccountId(),
                            command.clientReference()
                    ))
                    .retrieve()
                    .body(FinLedgerInitiateResponse.class);

            if (body == null) {
                throw new IllegalStateException("FinLedger returned empty initiate response");
            }
            return new InitiateRailPaymentResult(
                    body.instructionId(),
                    body.railReference(),
                    body.status(),
                    body.initiateJournalEntryId(),
                    body.replayed()
            );
        });
    }

    @Override
    public ConfirmSettlementResult confirmSettlement(ConfirmSettlementCommand command) {
        return execute(() -> {
            FinLedgerSettleResponse body = restClient.post()
                    .uri(
                            "/api/v1/tenants/{tenantId}/rails/payments/{railReference}/settle",
                            command.tenantId(),
                            command.railReference()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", command.idempotencyKey())
                    .header("Authorization", "Bearer " + command.bearerToken())
                    .body(new FinLedgerSettleRequest())
                    .retrieve()
                    .body(FinLedgerSettleResponse.class);

            if (body == null) {
                throw new IllegalStateException("FinLedger returned empty settle response");
            }
            return new ConfirmSettlementResult(
                    body.railReference() != null ? body.railReference() : command.railReference(),
                    body.status(),
                    body.replayed()
            );
        });
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        return execute(() -> {
            FinLedgerRefundResponse body = restClient.post()
                    .uri("/api/v1/tenants/{tenantId}/refunds", command.tenantId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", command.idempotencyKey())
                    .header("Authorization", "Bearer " + command.bearerToken())
                    .body(new FinLedgerRefundRequest(
                            command.transactionReference(),
                            command.originalJournalEntryId(),
                            command.refundAmount(),
                            command.currencyCode()
                    ))
                    .retrieve()
                    .body(FinLedgerRefundResponse.class);

            if (body == null) {
                throw new IllegalStateException("FinLedger returned empty refund response");
            }
            return new RefundResult(body.refundId(), body.status(), body.replayed());
        });
    }

    @Override
    public void putFeeConfig(PutFeeConfigCommand command) {
        execute(() -> {
            restClient.put()
                    .uri("/api/v1/tenants/{tenantId}/fee-config", command.tenantId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + command.bearerToken())
                    .body(new FinLedgerFeeConfigRequest(command.feeReversalPolicy()))
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    private <T> T execute(java.util.function.Supplier<T> supplier) {
        try {
            return resilience.execute(DEPENDENCY, supplier);
        } catch (BulkheadFullException | CallNotPermittedException ex) {
            throw new RetryableDependencyException(DEPENDENCY, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            if (isTimeout(ex)) {
                throw new RetryableDependencyException(DEPENDENCY, "FinLedger call timed out", ex);
            }
            throw ex;
        }
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof TimeoutException || cur instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            if (cur.getClass().getName().contains("Timeout")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    record FinLedgerInitiateRequest(
            String railCode,
            String amount,
            String currencyCode,
            UUID clearingAccountId,
            UUID counterpartyAccountId,
            String clientReference
    ) {
    }

    record FinLedgerInitiateResponse(
            UUID instructionId,
            String railReference,
            String status,
            UUID initiateJournalEntryId,
            boolean replayed
    ) {
    }

    record FinLedgerSettleRequest() {
    }

    record FinLedgerSettleResponse(
            String railReference,
            String status,
            boolean replayed
    ) {
    }

    record FinLedgerRefundRequest(
            String transactionReference,
            UUID originalJournalEntryId,
            String refundAmount,
            String currencyCode
    ) {
    }

    record FinLedgerRefundResponse(
            UUID refundId,
            String status,
            boolean replayed
    ) {
    }

    record FinLedgerFeeConfigRequest(String feeReversalPolicy) {
    }
}
