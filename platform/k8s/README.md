# PayHub Kubernetes (DS-019)

Exit-focused kind lab: Temporal + two `payment-orchestrator` workers.

- Kind config: [`kind/kind-config.yaml`](kind/kind-config.yaml)
- Kustomize base: [`base/`](base/)
- Overlays: [`overlays/ghcr`](overlays/ghcr/) (GHCR `0.1.0`), [`overlays/local-load`](overlays/local-load/) (`kind load`)
- Runbook / exit checklist: [`docs/runbooks/kind-temporal-failover.md`](../../docs/runbooks/kind-temporal-failover.md)

```bash
kind create cluster --name payhub --config platform/k8s/kind/kind-config.yaml
kubectl apply -k platform/k8s/overlays/ghcr
```
