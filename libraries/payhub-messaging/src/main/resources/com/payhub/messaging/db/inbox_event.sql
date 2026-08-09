-- Canonical PayHub inbox_event (DS-007). Copy into each service Flyway migration.
CREATE TABLE inbox_event (
    event_id UUID PRIMARY KEY,
    consumer_name VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
