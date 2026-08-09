# ADR-006 — PayHub messaging helpers (inbox / outbox)

- **Status:** Accepted
- **Date:** 2026-08-09
- **Ticket:** DS-007

## Context

DS-005 proved inbox dedup for FinLedger CDC → Reporting. PayHub services still need a
**shared technical shape** for their own outbox (e.g. Orchestrator →
`payment.lifecycle.v1`) and inbox consumers, without a shared domain module
(plan §2.3 / ADR-002).

## Decision

1. **`libraries/payhub-messaging`:** narrow JAR with `EventEnvelope`, `InboxStore`,
   `OutboxWriter`, and canonical SQL for `inbox_event` / `outbox_event`. No payment,
   merchant, or ledger domain types.
2. **PayHub outbox relay:** JDBC/scheduled **poller** publishes unpublished rows to
   Kafka. Debezium CDC remains FinLedger-only (ADR-004) until a proven need to CDC
   every PayHub database.
3. **Never dual-write:** aggregate mutation + outbox insert in one local Postgres TX;
   Kafka publish is after commit via the relay.
4. **Retries / poison (DS-016):** consumer-side finite Spring Kafka retries, then
   `{topic}.dlq` via `KafkaPoisonHandlers` / `DeadLetterPublishingRecoverer`. Dedicated
   delayed `*.retry.*` topic chains remain deferred. Replay is an ops use case on Reporting
   (`POST /api/v1/reporting/events/dlq/replay`); see
   [`docs/runbooks/kafka-poison-messages.md`](../runbooks/kafka-poison-messages.md).

## Consequences

- Services own Flyway copies of the canonical SQL and their JPA adapters.
- Reporting journal inbox (DS-005) stays compatible; lifecycle events reuse the same
  inbox table / port semantics.
- Rejected: shared domain JAR, dual-write from use case to Kafka, Debezium on every
  PayHub DB in DS-007.
- Optional `spring-kafka` on `payhub-messaging` for `KafkaPoisonHandlers` only; services
  that do not wire poison handlers do not need to use that type.
