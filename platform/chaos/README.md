# PayHub chaos / load (DS-017)

Codified fault-injection experiments and a light capacity report. These are **on-demand**
(`@Tag("chaos")`) — they do not block normal PR CI.

## Run

Default Surefire excludes `chaos`:

```bash
./mvnw -pl services/payment-orchestrator test
```

Run the chaos suite (clears the exclude):

```bash
./mvnw -pl services/payment-orchestrator -Pchaos test
```

Requires Docker (Testcontainers Toxiproxy).

## Experiments

| Experiment | Hypothesis | Exit check |
|------------|------------|------------|
| [FinLedger timeout / cut — no duplicate](experiments/finledger-timeout-no-duplicate.md) | Network faults + retries keep one `Idempotency-Key`; at most one create | Single `201` create; replay uses same key |

## Capacity

See [capacity-report.md](capacity-report.md) (DS-015 bulkhead evidence + how to re-measure).

## Out of scope here

- Chaos Mesh / pod kills → DS-019+
- Full k6/Gatling product load suite
- DR zone loss → DS-023
