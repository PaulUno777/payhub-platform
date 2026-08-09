# Capacity report (DS-017, light)

Learning-grade snapshot — not a full load-test product suite.

## DS-015 evidence (rail vs FinLedger isolation)

**Test:** `RailBulkheadDoesNotStarveFinLedgerTest` (`@Tag("integration")`)  
**Service:** `payment-orchestrator`

| Setting | Value |
|---------|-------|
| Rail bulkhead max concurrent | 2 |
| Concurrent slow rail submits | 4 (held until latch) |
| FinLedger settles during rail pressure | All succeed |

**Verdict:** A saturated / slow rail pool does not starve FinLedger calls (separate HTTP
pools + bulkheads). Re-run:

```bash
./mvnw -pl services/payment-orchestrator test -Dtest=RailBulkheadDoesNotStarveFinLedgerTest
```

## DS-017 chaos (no duplicate money effect)

**Test:** `NoDuplicateFinLedgerEffectUnderToxiproxyIT` (`@Tag("chaos")`)  
**Metric:** create count under Toxiproxy cut + restore (pass/fail; no p95 required at this N).

```bash
./mvnw -pl services/payment-orchestrator -Pchaos test
```

**Verdict:** After network cut and restore, one `Idempotency-Key` → one FinLedger create;
subsequent call is replay only. See
[experiments/finledger-timeout-no-duplicate.md](experiments/finledger-timeout-no-duplicate.md).

## Non-goals (deferred)

- Sustained RPS / latency SLOs (DS-021)
- Cluster-wide chaos (Chaos Mesh — DS-019+)
- Multi-service soak with real FinLedger image under Toxiproxy in Compose
