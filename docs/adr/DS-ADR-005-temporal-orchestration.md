# ADR-005 — Temporal for payment/refund orchestration

- **Status:** Accepted
- **Date:** 2026-08-09
- **Ticket:** DS-006

## Context

PayHub’s payment and refund paths span Risk, Rail, and FinLedger with explicit
compensation and human review waits (plan §4). Pure Kafka choreography would
scatter timeouts, ordering, and compensation across many consumers. The
alternative is a durable orchestrator that owns the saga timeline without 2PC/XA.

## Decision

1. **Temporal** hosts `PaymentCaptureWorkflow` and (later) `RefundWorkflow` in
   `payment-orchestrator` behind `WorkflowPort`.
2. **DS-006 scope:** start the capture workflow at `CreatePayment`; sync
   `EvaluateRisk` stays in the HTTP use case for latency-bound responses; the
   workflow **parks** until a later signal / activities (DS-008+).
3. **Compose:** pinned `temporalio/auto-setup` + dedicated Postgres — never
   `:latest`.
4. Rejected for v1 payment/refund: full choreography-only saga, Camunda as the
   default engine, or embedding rail/ledger calls solely in Kafka consumers.

## Consequences

- Orchestrator depends on `io.temporal:temporal-sdk` (and `temporal-testing` for
  tests). Local/CI may disable Temporal via `payhub.temporal.enabled=false`
  (`NoOpWorkflowPort`).
- Saga steps after risk remain Temporal activities; FinLedger stays behind
  `LedgerPort` only — Temporal never replaces the REST integration boundary.
- Ops human review (`RISK_REVIEW`) can later signal the same workflow without
  inventing a second coordination bus.
