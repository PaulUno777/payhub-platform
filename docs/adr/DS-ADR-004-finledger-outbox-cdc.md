# ADR-004 — FinLedger outbox CDC via Debezium (no SPI)

- **Status:** Accepted
- **Date:** 2026-08-09
- **Ticket:** DS-005

## Context

PayHub must stream FinLedger journal posts onto Kafka topic `ledger.journal-entry.v1`
(plan §6.1) without forking FinLedger or plugging into its in-process `EventPublisher`
(plan §0.1 / §5). FinLedger already writes a transactional `outbox_event` row in the same
Postgres TX as the journal entry and runs a local poller that only logs / triggers
optional fraud async.

## Decision

1. **CDC path:** Debezium Postgres connector reads the FinLedger WAL (`wal_level=logical`)
   for table `public.outbox_event` only. PayHub never opens FinLedger’s DB for application
   queries.
2. **Routing:** Debezium Outbox `EventRouter` maps FinLedger columns → Kafka:
   - `id` → event id (header)
   - `aggregate_id` → Kafka key (`journalEntryId`)
   - `event_type` → filter / route field (`TransactionPosted`)
   - `payload` → message body (JSON = FinLedger `TransactionPosted` fields)
   - `tenant_id` → Kafka header `tenantId`
   - Topic replacement: always `ledger.journal-entry.v1`
3. **Schema Registry:** Confluent Schema Registry in Compose; **JSON Schema** for DS-005
   (KISS vs Avro). Consumers may deserialize JSON without a generated Avro binding.
4. **Coexistence:** FinLedger’s `OutboxPoller` + `LoggingEventPublisher` remain unchanged.
   CDC does not replace FinLedger’s internal poller; it is the only path into PayHub Kafka.
5. **First consumer:** `reporting-service` inbox + non-authoritative projection (Redis later,
   DS-013).

## Consequences

- Postgres FinLedger Compose service must run with `wal_level=logical`.
- Connector registration is operational (`platform/kafka/register-connector.sh`), not baked
  into the FinLedger image.
- Duplicate Kafka delivery is handled by the consumer **inbox** (exit criterion DS-005);
  DS-007 may later standardize inbox/outbox helpers across services.
- Rejects: FinLedger SPI/plugin, shared “ledger SDK”, dual-write from PayHub into Kafka for
  FinLedger journal events.
