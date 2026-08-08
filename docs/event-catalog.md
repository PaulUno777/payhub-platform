# Event catalog (v1)

Normative detail lives in plan §6.1. AsyncAPI specs land under `contracts/` from DS-005.

| Topic | Producer | Key | Consumers | Retention |
|-------|----------|-----|-----------|-----------|
| `payment.lifecycle.v1` | Orchestrator | `paymentId` | Reporting, Notification, Reconciliation | 30d |
| `ledger.journal-entry.v1` | FinLedger CDC | `journalEntryId` | Reporting, Reconciliation | 90d |
| `rail.operation.v1` | Rail Adapter | `railOperationId` | Orchestrator, Reconciliation | 30d |
| `*.retry.*` / `*.dlq` | consumers | original key | replay tooling | dedicated |

**Not in v1:** `tenant.events.v1`, `ledger.account-impact.v1` (plan §19).

Envelope (minimum): `eventId`, `eventType`, `schemaVersion`, `occurredAt`, `producer`,
`aggregateId`, `tenantId`, `traceparent`, `causationId`, minimal PII-free payload.

Notification delivers merchant webhooks via RabbitMQ **work queues** (lab, plan §6.3) —
it does **not** consume FinLedger’s outbox directly.
