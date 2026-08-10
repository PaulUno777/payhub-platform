# PayHub Kubernetes (DS-019+)

**Default topology:** [kind](https://kind.sigs.k8s.io/) **single-node** (ADR-008 amended).
Multi-node is an ephemeral lab later (post-DS-023 / consensus) — not the laptop default.

## DNS parity (Compose ↔ kind)

**Contract:** every kind `Service` `metadata.name` equals the Compose service key and the
hostname in [`platform/config/*-compose.yml`](../config/). Catalog:
[`platform/ports.md`](../ports.md) (section *Kind CoreDNS*).

- Apps use short names (`http://payment-orchestrator:8080`, `temporal:7233`) — same as Compose.
- FQDN if needed: `<name>.payhub.svc.cluster.local`.
- Rename rule: change Compose + kind Service + compose YAML + `ports.md` together.
- Images: same Dockerfiles / GHCR `:semver` or `:local` as Compose; infra pins match
  `platform/compose/docker-compose.yml`.
- ConfigMaps project compose hostnames (not a second naming scheme). ADR-003 amended.

DS-019 lab already uses Compose-identical names: `payment-orchestrator`,
`postgres-orchestrator`, `postgres-temporal`, `temporal`.

## Scope by ticket

| Ticket | What lives here |
|--------|-----------------|
| **DS-019** | Exit-focused: Temporal + Postgres Temporal + Postgres Orchestrator + **2×** `payment-orchestrator` + PDB. No full mesh, HPA/KEDA, or Argo. |
| **DS-020** | Mesh on the same single-node cluster under a documented RAM budget (Postgres-per-service tuning, Kafka KRaft 1 broker, JVM SerialGC, Zitadel → `postgres-zitadel`). Names from `ports.md` (`postgres-rail`, not `postgres-railadapter`). Proportional data-safety — not laptop multi-node HA. |
| **DS-021+** | SRE (Prometheus/Grafana); GitOps (Argo/Flux) after useful alerts — not day-1 of DS-019. |

## Diagrams

- Logical topology (services / Kafka / FinLedger / Temporal / IdP): [`docs/diagrams/topology.mermaid`](../../docs/diagrams/topology.mermaid)
- Namespace phases (`ds019_lab` vs `ds020_mesh_target`): [`docs/diagrams/k8s-namespace.mermaid`](../../docs/diagrams/k8s-namespace.mermaid)

## Layout

- Kind config: [`kind/kind-config.yaml`](kind/kind-config.yaml)
- Kustomize base: [`base/`](base/)
- Overlays: [`overlays/ghcr`](overlays/ghcr/) (GHCR semver), [`overlays/local-load`](overlays/local-load/) (`kind load`)
- Runbook / DS-019 exit checklist: [`docs/runbooks/kind-temporal-failover.md`](../../docs/runbooks/kind-temporal-failover.md)
- Plan B if OOM: [`docs/OPEN_QUESTIONS.md`](../../docs/OPEN_QUESTIONS.md) Q18

```bash
kind create cluster --name payhub --config platform/k8s/kind/kind-config.yaml
kubectl apply -k platform/k8s/overlays/local-load   # or overlays/ghcr after a matching release tag
```
