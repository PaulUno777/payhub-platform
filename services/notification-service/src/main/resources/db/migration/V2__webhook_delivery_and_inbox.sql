CREATE TABLE inbox_event (
    event_id UUID PRIMARY KEY,
    consumer_name VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE webhook_delivery (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    payment_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    payment_status VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    endpoint_url VARCHAR(2048) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INT NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_webhook_delivery_due
    ON webhook_delivery (status, next_attempt_at);

CREATE INDEX idx_webhook_delivery_tenant_status
    ON webhook_delivery (tenant_id, status);
