package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

/**
 * Anti-corruption port for FinLedger payment/refund REST (Orchestrator only).
 */
public interface LedgerPort {

    InitiateRailPaymentResult initiateRailPayment(InitiateRailPaymentCommand command);

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
}
