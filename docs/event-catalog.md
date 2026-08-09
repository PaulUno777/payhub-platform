# Event catalog (v1)

Normative detail lives in plan §6.1.

AsyncAPI:
- [`contracts/ledger-journal-entry/asyncapi.yaml`](../contracts/ledger-journal-entry/asyncapi.yaml)
  — `ledger.journal-entry.v1` (DS-005). CDC: [`platform/kafka/`](../platform/kafka/),
  [ADR-004](adr/DS-ADR-004-finledger-outbox-cdc.md).
- [`contracts/payment-lifecycle/asyncapi.yaml`](../contracts/payment-lifecycle/asyncapi.yaml)
  — `payment.lifecycle.v1` (DS-007). Orchestrator outbox + poller; helpers in
  `libraries/payhub-messaging` ([ADR-006](adr/DS-ADR-006-payhub-messaging-outbox-inbox.md)).

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
