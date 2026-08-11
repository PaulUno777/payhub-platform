# Kind mesh + proportional data safety (DS-020)

Exit criteria (plan §17):

1. Mesh boots on the **same** kind single-node cluster under documented RAM levers,
   **without recurrent OOMKiller**.
2. One restore of `postgres-finledger` + Debezium resume shows **no functional
   divergence** (Reporting inbox skips a replayed `eventId`).

Precondition: DS-019 lab works. Images: `ghcr.io/pauluno777/payhub-*:0.2.0` (no new
release unless a service image must change). If the kind node cannot pull registries,
`kind load` from the host first (`imagePullPolicy: IfNotPresent`).

Out of scope: RabbitMQ, Jaeger, Prometheus, Argo, HPA, multi-node HA.

## 1. Apply mesh

```bash
kind create cluster --name payhub --config platform/k8s/kind/kind-config.yaml   # if needed
# optional: kind load docker-image postgres:17-alpine apache/kafka:3.8.1 ... --name payhub

kubectl apply -k platform/k8s/overlays/mesh
kubectl -n payhub rollout status deploy/kafka --timeout=180s
kubectl -n payhub rollout status deploy/postgres-finledger --timeout=180s
kubectl -n payhub rollout status deploy/finledger --timeout=240s
kubectl -n payhub rollout status deploy/debezium-connect --timeout=240s
kubectl -n payhub rollout status deploy/reporting-service --timeout=300s
kubectl -n payhub get pods
```

Expect Job `register-finledger-outbox` Completed (registers `finledger-outbox`).

## 2. RAM levers (applied in manifests)

| Lever | Where |
|-------|--------|
| Postgres `max_connections=20 shared_buffers=32MB work_mem=4MB` | every `postgres-*` |
| Kafka KRaft 1 broker, heap 256 Mi | `kafka` |
| JVM `-XX:+UseSerialGC -XX:MaxRAMPercentage=75.0` | PayHub apps + FinLedger |
| Zitadel on `postgres-zitadel` (not Cockroach) | ADR-007 |
| Tight requests/limits | all Deployments |

If OOMKiller loops after this: stop BFFs / schema-registry first; cloud Plan B only
after [OPEN_QUESTIONS Q18](../OPEN_QUESTIONS.md) checkboxes.

Record a session (fill after apply):

| Source | Value |
|--------|--------|
| Docker Desktop VM | 7.652 GiB (`docker info`, 2026-08-11 lab) |
| `kubectl -n payhub top pods` sum | metrics-server not installed on kind — use `docker stats` if needed |
| OOMKilled count (`kubectl get pods`) | 0 on first mesh apply (Kafka needed `CLUSTER_ID` + `1@localhost:9093`; not OOM) |

## 3. Restore / CDC drill

`postgres-finledger` uses a PVC. Dump/restore that database; inbox on Reporting
prevents a second functional apply of the same `eventId`.

```bash
# 3a. Baseline: count journal projections (0 is fine if no traffic yet)
kubectl -n payhub exec deploy/postgres-reporting -- \
  psql -U payhub -d reporting -c "SELECT count(*) FROM journal_entry_projection;" \
  || kubectl -n payhub exec deploy/postgres-reporting -- \
       psql -U payhub -d reporting -c "\dt"

# 3b. Seed one outbox row on FinLedger (id must be a UUID)
EVENT_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
kubectl -n payhub exec deploy/postgres-finledger -- \
  psql -U finledger -d finledger -v ON_ERROR_STOP=1 -c "
    INSERT INTO outbox_event (id, aggregate_id, event_type, payload, tenant_id, created_at)
    SELECT '${EVENT_ID}', '${EVENT_ID}', 'JournalEntryPosted',
           '{\"eventId\":\"${EVENT_ID}\",\"payload\":{}}'::jsonb,
           '00000000-0000-0000-0000-000000000001', now()
    WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='outbox_event');
  "

# Wait for CDC → Reporting (or skip seed if outbox schema differs — then use an
# existing eventId from SELECT id FROM outbox_event LIMIT 1).

# 3c. Dump
kubectl -n payhub exec deploy/postgres-finledger -- \
  pg_dump -U finledger -d finledger -Fc -f /tmp/finledger.dump
kubectl -n payhub cp payhub/$(kubectl -n payhub get pod -l app.kubernetes.io/name=postgres-finledger -o jsonpath='{.items[0].metadata.name}'):/tmp/finledger.dump /tmp/finledger.dump

# 3d. Restore into the same PVC (scale writers down first)
kubectl -n payhub scale deploy/finledger deploy/debezium-connect --replicas=0
kubectl -n payhub exec deploy/postgres-finledger -- \
  pg_restore -U finledger -d finledger --clean --if-exists /tmp/finledger.dump \
  || (kubectl -n payhub cp /tmp/finledger.dump payhub/$(kubectl -n payhub get pod -l app.kubernetes.io/name=postgres-finledger -o jsonpath='{.items[0].metadata.name}'):/tmp/finledger.dump && \
      kubectl -n payhub exec deploy/postgres-finledger -- pg_restore -U finledger -d finledger --clean --if-exists /tmp/finledger.dump)

# Recreate replication slot if restore dropped it
kubectl -n payhub exec deploy/postgres-finledger -- \
  psql -U finledger -d finledger -c "SELECT pg_drop_replication_slot('payhub_finledger_outbox');" || true

kubectl -n payhub scale deploy/finledger deploy/debezium-connect --replicas=1
kubectl -n payhub delete job register-finledger-outbox --ignore-not-found
kubectl apply -k platform/k8s/overlays/mesh

# 3e. After connector is RUNNING, inbox must still have a single row per eventId
kubectl -n payhub exec deploy/postgres-reporting -- \
  psql -U payhub -d reporting -c "SELECT event_id, count(*) FROM inbox_event GROUP BY event_id HAVING count(*) > 1;"
# expect 0 rows
```

## Exit checklist (DS-020)

- [x] `kubectl apply -k platform/k8s/overlays/mesh` — infra + Kafka Ready; app pods wait on GHCR/`kind load` (IfNotPresent)
- [x] RAM table filled; no recurrent OOMKilled (0 this session)
- [x] PVC `postgres-finledger-data` bound (1 Gi, local-path)
- [x] Dump → restore: marker row returned to `before-dump` after `--clean` restore
- [ ] Inbox: no duplicate `event_id` — run §3e once `debezium-connect` is Running and the register Job has Completed

When the last box is checked, mark DS-020 **done** in `docs/development.md`. Do not start DS-021 until then.

## Tear down

```bash
kubectl delete -k platform/k8s/overlays/mesh
# or: kind delete cluster --name payhub
```
