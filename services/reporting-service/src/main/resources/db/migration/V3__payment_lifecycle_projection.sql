CREATE TABLE payment_lifecycle_projection (
    payment_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    status VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    as_of TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_lifecycle_projection_tenant ON payment_lifecycle_projection (tenant_id);
