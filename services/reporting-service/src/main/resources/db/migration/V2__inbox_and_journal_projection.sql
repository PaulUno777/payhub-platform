CREATE TABLE inbox_event (
    event_id UUID PRIMARY KEY,
    consumer_name VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE journal_entry_projection (
    journal_entry_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    transaction_reference VARCHAR(255) NOT NULL,
    entry_type VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    as_of TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_journal_entry_projection_tenant ON journal_entry_projection (tenant_id);
