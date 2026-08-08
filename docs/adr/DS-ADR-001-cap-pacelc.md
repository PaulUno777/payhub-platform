# DS-ADR-001 — CAP / PACELC choices for PayHub components

- **Status:** Draft (DS-001)
- **Date:** 2026-08-08
- **Deciders:** PayHub maintainers

## Context

PayHub spans Orchestrator, Rail, FinLedger (external), Reporting, and BFFs. Partition and
latency trade-offs must be explicit so timeouts never silently become financial verdicts.

## Decision

| Component | During partition (CAP) | Outside partition (PACELC) |
|-----------|------------------------|----------------------------|
| FinLedger (external) | Refuse writes if preconditions unverifiable | Accept latency of consistent TX |
| Payment Orchestrator | Persist reached state; never conclude for the rail | Prefer durable Temporal workflow over long sync calls |
| Rail Adapter | Keep ambiguity; poll / reconcile | Prefer idempotent confirm over optimistic success |
| Reporting / BFF reads | Serve last view with staleness | Prefer low latency/cache; never financial truth |

**ACID** protects each local critical decision (aggregate + idempotency + outbox in one
Postgres TX). **BASE** describes projections, notifications, and reconciliation.

Ambiguous rail outcomes → `RECONCILIATION_REQUIRED` (never direct `FAILED_FINAL`).

## Alternatives considered

| Option | Why not |
|--------|---------|
| CP everywhere with 2PC/XA | Extends locks across PSP; unavailable under partition |
| AP optimistic “success” on timeout | Creates false financial finals |
| Treat Reporting as CP source of truth | Violates FinLedger-as-sole-monetary-truth |

## Consequences

- Positive: interview-defensible, testable timeout behaviour (DS-008 exit)
- Trade-off: operators must close Breaks; happy-path latency includes async rail
- Follow-up: SLOs in DS-020; chaos proving no duplicate money effect in DS-017

## References

- `docs/PLAN_PAYHUB.md` §3.3, §1.4, §4.1
- FinLedger `docs/INTEGRATION_GUIDE.md` (idempotency / tenant mismatch)
