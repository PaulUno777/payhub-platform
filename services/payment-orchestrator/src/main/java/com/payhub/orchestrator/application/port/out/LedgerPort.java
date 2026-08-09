package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

/**
 * Anti-corruption port for FinLedger payment/refund REST (Orchestrator only).
 */
public interface LedgerPort {

    InitiateRailPaymentResult initiateRailPayment(InitiateRailPaymentCommand command);

    ConfirmSettlementResult confirmSettlement(ConfirmSettlementCommand command);

    RefundResult refund(RefundCommand command);

    void putFeeConfig(PutFeeConfigCommand command);

    record InitiateRailPaymentCommand(
            UUID tenantId,
            String idempotencyKey,
            String railCode,
            String amount,
            String currencyCode,
            UUID clearingAccountId,
            UUID counterpartyAccountId,
            String clientReference,
            String bearerToken
    ) {
    }

    record InitiateRailPaymentResult(
            UUID instructionId,
            String railReference,
            String status,
            UUID initiateJournalEntryId,
            boolean replayed
    ) {
    }

    record ConfirmSettlementCommand(
            UUID tenantId,
            String railReference,
            String idempotencyKey,
            String bearerToken
    ) {
    }

    record ConfirmSettlementResult(
            String railReference,
            String status,
            boolean replayed
    ) {
    }

    record RefundCommand(
            UUID tenantId,
            String idempotencyKey,
            String transactionReference,
            UUID originalJournalEntryId,
            String refundAmount,
            String currencyCode,
            String bearerToken
    ) {
    }

    record RefundResult(
            UUID refundId,
            String status,
            boolean replayed
    ) {
    }

    record PutFeeConfigCommand(
            UUID tenantId,
            String feeReversalPolicy,
            String bearerToken
    ) {
    }
}
