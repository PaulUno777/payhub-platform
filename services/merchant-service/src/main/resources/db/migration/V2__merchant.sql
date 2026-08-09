CREATE TABLE merchant (
    id UUID PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tier VARCHAR(32) NOT NULL,
    assigned_rule_set_key VARCHAR(128) NOT NULL,
    fin_ledger_tenant_id UUID,
    merchant_wallet_id UUID,
    settlement_wallet_id UUID,
    rejection_reason VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE idempotency_record (
    operation VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(256) NOT NULL,
    merchant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (operation, idempotency_key)
);
