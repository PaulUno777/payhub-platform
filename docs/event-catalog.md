# Event catalog (v1)

Normative detail lives in plan §6.1.

AsyncAPI (DS-005):
[`contracts/ledger-journal-entry/asyncapi.yaml`](../contracts/ledger-journal-entry/asyncapi.yaml)
for `ledger.journal-entry.v1`. CDC wiring: [`platform/kafka/`](../platform/kafka/),
[ADR-004](adr/DS-ADR-004-finledger-outbox-cdc.md).

| Topic | Producer | Key | Consumers | Retention |
|-------|----------|-----|-----------|-----------|
| `payment.lifecycle.v1` | Orchestrator | `paymentId` | Reporting, Notification, Reconciliation | 30d |
| `ledger.journal-entry.v1` | FinLedger CDC | `journalEntryId` | Reporting, Reconciliation | 90d |
| `rail.operation.v1` | Rail Adapter | `railOperationId` | Orchestrator, Reconciliation | 30d |
| `*.retry.*` / `*.dlq` | consumers | original key | replay tooling | dedicated |

**Not in v1:** `tenant.events.v1`, `ledger.account-impact.v1` (plan §19).

Envelope (minimum): `eventId`, `eventType`, `schemaVersion`, `occurredAt`, `producer`,
`aggregateId`, `tenantId`, `traceparent`, `causationId`, minimal PII-free payload.

`TransactionPosted` payload fields are owned by FinLedger (not reinvented in PayHub).

Notification delivers merchant webhooks via RabbitMQ **work queues** (lab, plan §6.3) —
it does **not** consume FinLedger’s outbox directly.
