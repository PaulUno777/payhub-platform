-- ledger refs + refund tracking for DS-009

ALTER TABLE payment ADD COLUMN IF NOT EXISTS rail_reference VARCHAR(128);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS initiate_journal_entry_id UUID;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE payment ALTER COLUMN status TYPE VARCHAR(40);
