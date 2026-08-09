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
}
