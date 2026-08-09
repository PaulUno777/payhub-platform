# Experiment: FinLedger path cut — no duplicate financial effect

**Ticket:** DS-017  
**Code:** `NoDuplicateFinLedgerEffectUnderToxiproxyIT`  
**Tag:** `@Tag("chaos")`

## Hypothesis

When the Orchestrator → FinLedger TCP path is cut via Toxiproxy and later restored,
activity-style retries of `initiateRailPayment` with the **same** `Idempotency-Key`
produce at most **one** successful create (`201`, `replayed=false`). A further call is
a replay (`200`, `replayed=true`), not a second financial instruction.

## Mechanism

1. MockWebServer stands in for FinLedger on the host.
2. Toxiproxy (Testcontainers) proxies to MockWebServer on the host via
   `host.docker.internal` (Docker Desktop).
3. `proxy.disable()` simulates a hard cut; `proxy.enable()` restores the path.
4. First initiate fails (transport error while cut).
5. Client rebuilt after restore (drop poisoned pooled connections); second initiate
   succeeds once (`201`).
6. Third initiate returns FinLedger replay (`200`, `replayed=true`).

## Pass / fail

| Pass | Fail |
|------|------|
| All requests share one `Idempotency-Key` | Different keys across retries |
| Exactly one response body with `replayed=false` create semantics (one `201`) | Two or more creates |
| Final call reports `replayed=true` | Second create or lost effect |

## Blast radius

- **In:** Orchestrator `FinLedgerClient` / `LedgerPort` only.
- **Out:** Rail adapter, Reporting, Notification, Kafka consumers, FinLedger process itself
  (mocked).

## How to run

```bash
./mvnw -pl services/payment-orchestrator -Pchaos test
```
