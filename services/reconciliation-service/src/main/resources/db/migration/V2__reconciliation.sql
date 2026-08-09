-- DS-010 reconciliation domain tables

CREATE TABLE reconciliation_run (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rail_code VARCHAR(64) NOT NULL,
    statement_key VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_recon_run_open_tenant_rail
    ON reconciliation_run (tenant_id, rail_code)
    WHERE status = 'OPEN';

CREATE TABLE statement_import (
    statement_key VARCHAR(128) PRIMARY KEY,
    imported_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE recon_break (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES reconciliation_run (id),
    payment_id UUID NOT NULL,
    external_ref VARCHAR(128) NOT NULL,
    break_type VARCHAR(64) NOT NULL,
    detail VARCHAR(512) NOT NULL,
    status VARCHAR(16) NOT NULL,
    resolution_action VARCHAR(32),
    resolved_by VARCHAR(128),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_recon_break_run ON recon_break (run_id);
CREATE INDEX idx_recon_break_status ON recon_break (status);
