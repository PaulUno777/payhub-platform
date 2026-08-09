package com.payhub.orchestrator.infrastructure.ledger;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.LedgerCredentialsPort;

@Component
public class FinLedgerCredentialsAdapter implements LedgerCredentialsPort {

    private final FinLedgerProperties properties;

    public FinLedgerCredentialsAdapter(FinLedgerProperties properties) {
        this.properties = properties;
    }

    @Override
    public String bearerToken() {
        return properties.bearerToken();
    }

    @Override
    public String railCode() {
        return properties.railCode();
    }

    @Override
    public UUID clearingAccountId() {
        return properties.clearingAccountId();
    }

    @Override
    public UUID counterpartyAccountId() {
        return properties.counterpartyAccountId();
    }
}
