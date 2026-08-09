package com.payhub.orchestrator.infrastructure.ledger;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.application.port.out.LedgerPort;

@Component
public class FinLedgerClient implements LedgerPort {

    private final RestClient restClient;

    public FinLedgerClient(RestClient.Builder restClientBuilder, FinLedgerProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(properties.baseUrl(), "payhub.finledger.base-url"))
                .build();
    }

    @Override
    public InitiateRailPaymentResult initiateRailPayment(InitiateRailPaymentCommand command) {
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
    }

    @Override
    public ConfirmSettlementResult confirmSettlement(ConfirmSettlementCommand command) {
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
    }

    @Override
    public RefundResult refund(RefundCommand command) {
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
    }

    @Override
    public void putFeeConfig(PutFeeConfigCommand command) {
        restClient.put()
                .uri("/api/v1/tenants/{tenantId}/fee-config", command.tenantId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + command.bearerToken())
                .body(new FinLedgerFeeConfigRequest(command.feeReversalPolicy()))
                .retrieve()
                .toBodilessEntity();
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
