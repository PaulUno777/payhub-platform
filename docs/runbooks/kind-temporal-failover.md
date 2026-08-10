# Kind + Temporal worker failover (DS-019)

Exit criterion: *a Temporal worker pod killed mid-saga is resumed by another worker.*

Precondition: DS-018 images on GHCR (`payhub-payment-orchestrator:0.1.0`) or a local
`:local` image loaded into kind (ADR-008).

This stack is **exit-focused**: Temporal + orchestrator Postgres + **2** orchestrator
replicas. Full PayHub mesh (gateway, Kafka, FinLedger, …) stays on Compose for now.

## Prerequisites

- Docker
- [kind](https://kind.sigs.k8s.io/) ≥ 0.20
- `kubectl`
- Optional: `curl` + `jq` for the smoke steps

## 1. Create cluster

```bash
kind create cluster --name payhub --config platform/k8s/kind/kind-config.yaml
kubectl cluster-info --context kind-payhub
```

## 2. Apply manifests

**Local iteration / exit proof on this branch (required until the next image tag includes
`continue-capture` + `payhub.risk.in-memory-default-decision`):**

```bash
docker build -f services/payment-orchestrator/Dockerfile \
  -t pauluno/payhub-payment-orchestrator:local .
kind load docker-image pauluno/payhub-payment-orchestrator:local --name payhub
kubectl apply -k platform/k8s/overlays/local-load
```

**Prod-like pull** (use after a release that includes the DS-019 orchestrator changes):

```bash
# If GHCR packages are private:
#   kubectl create secret docker-registry ghcr-pull -n payhub \
#     --docker-server=ghcr.io --docker-username=USER --docker-password=TOKEN
# then patch the Deployment to use imagePullSecrets: [ghcr-pull]

kubectl apply -k platform/k8s/overlays/ghcr
```

Wait until ready:

```bash
kubectl -n payhub rollout status deploy/postgres-temporal
kubectl -n payhub rollout status deploy/temporal
kubectl -n payhub rollout status deploy/postgres-orchestrator
kubectl -n payhub rollout status deploy/payment-orchestrator
kubectl -n payhub get pods -o wide
```

Expect **2** `payment-orchestrator-*` pods Ready, plus Temporal and both Postgres pods.

## 3. Exit proof — kill worker mid-saga

The capture workflow parks at `Workflow.await` until `continueCapture` is signaled
([`PaymentCaptureWorkflowImpl`](../../services/payment-orchestrator/src/main/java/com/payhub/orchestrator/infrastructure/temporal/PaymentCaptureWorkflowImpl.java)).
That park window is enough to delete a worker pod and prove another replica resumes.

### 3a. Start a payment (workflow starts, parks — risk REVIEW in kind ConfigMap)

Kind sets `PAYHUB_RISK_IN_MEMORY_DEFAULT_DECISION=REVIEW` so submit does **not** auto-signal
`continueCapture`. The workflow stays on `Workflow.await` — the kill window for the exit.

```bash
IDEMP=$(uuidgen)
RESP=$(curl -sS -X POST "http://127.0.0.1:8080/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMP" \
  -d '{
    "merchantId": "00000000-0000-0000-0000-000000000010",
    "tenantId": "00000000-0000-0000-0000-000000000001",
    "amount": "10.00",
    "currencyCode": "USD",
    "clientReference": "ds019-failover"
  }')
echo "$RESP"
PAYMENT_ID=$(echo "$RESP" | jq -r .id)
# expect status RISK_REVIEW
```

Confirm two workers:

```bash
kubectl -n payhub get pods -l app.kubernetes.io/name=payment-orchestrator
```

### 3b. Kill one orchestrator pod

```bash
VICTIM=$(kubectl -n payhub get pods -l app.kubernetes.io/name=payment-orchestrator \
  -o jsonpath='{.items[0].metadata.name}')
kubectl -n payhub delete pod "$VICTIM"
kubectl -n payhub get pods -l app.kubernetes.io/name=payment-orchestrator -w
# wait until 2 Ready again (Deployment recreates the deleted pod)
```

### 3c. Signal continue on the surviving workers

```bash
curl -sS -o /dev/null -w "%{http_code}\n" -X POST \
  "http://127.0.0.1:8080/api/v1/payments/${PAYMENT_ID}/continue-capture"
# expect 202

curl -sS "http://127.0.0.1:8080/api/v1/payments/${PAYMENT_ID}" | jq .
# capture activities run on whichever worker is alive (in-memory rail)
```

Verify pods show progress:

```bash
kubectl -n payhub logs -l app.kubernetes.io/name=payment-orchestrator --tail=100
```

## Exit criterion checklist (DS-019)

- [ ] kind cluster `payhub` created from `platform/k8s/kind/kind-config.yaml`
- [ ] `kubectl apply -k platform/k8s/overlays/ghcr` (or `local-load`) — all pods Ready
- [ ] Two `payment-orchestrator` replicas on task queue `payment-capture`
- [ ] Payment/workflow started and parked (or in-flight)
- [ ] One orchestrator pod deleted; Deployment restores replica count
- [ ] Workflow resumed / completed via the surviving worker (signal + logs)

When checked, mark DS-019 **done** in `docs/development.md`. Do not start DS-020 until then.

## Tear down

```bash
kind delete cluster --name payhub
```

## Out of scope here

- Gateway / BFFs / Kafka / FinLedger / Zitadel in-cluster
- ArgoCD/Flux, HPA/KEDA, canary
- Full EcoPay E2E on Kubernetes (later tickets)
