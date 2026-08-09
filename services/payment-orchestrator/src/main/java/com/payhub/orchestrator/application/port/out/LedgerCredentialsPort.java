package com.payhub.orchestrator.application.port.out;

import java.util.UUID;

/**
 * Sandbox FinLedger call credentials for async capture activities (no HTTP request scope).
 */
public interface LedgerCredentialsPort {

    String bearerToken();

    String railCode();

    UUID clearingAccountId();

    UUID counterpartyAccountId();
}
