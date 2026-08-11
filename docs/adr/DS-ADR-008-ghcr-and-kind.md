# ADR-008 — PayHub images on GHCR; local cluster = kind (single-node default)

- **Status:** Amended
- **Date:** 2026-08-09
- **Amended:** 2026-08-10
- **Ticket:** DS-018 (registry); informs DS-019+ (Kubernetes)

## Context

`project-workflow.mdc` promised a `release.yml` that builds and pushes multi-arch images,
but only `ci.yml` (Maven test) existed. DS-019 (Kubernetes, formerly DS-018) cannot
claim a prod-like cluster if image publish is untested folklore. Folding registry + kind +
Temporal failover into one PR violates the one-concern rule (architecture §9 / DS-016
lesson). Docker Hub vs GHCR and kind vs k3d needed an explicit choice.

A later RAM review (Mac Intel 16 Go, Docker Desktop Linux VM ~8 Go) showed that a
**permanent multi-node** kind/kubeadm lab would spend memory on kubelet/containerd/CoreDNS
per node instead of PayHub services. Multi-node remains valuable as an **ephemeral** lab,
not as the day-1 local default.

## Decision

1. **Registry = GHCR** for PayHub service images:
   `ghcr.io/pauluno777/payhub-<service>:<semver>` (and matching digest tags as implemented
   in DS-018). Authentication in GitHub Actions uses `GITHUB_TOKEN` with
   `packages: write` — no `DOCKERHUB_TOKEN` for PayHub images.
2. **FinLedger** remains the pinned **Docker Hub** image (`unoteck/finledger` or current
   pin) — external dependency, unchanged. Two registries for two ownership boundaries is
   intentional, not drift.
3. **Tagging contract:**
   - Laptop / Compose: `…:local` from `docker build` at repo root
   - Releases: semver from git tags (e.g. `v0.1.0` → `:0.1.0`); never ship `:latest` as the
     only tag
4. **Local Kubernetes learning target = kind** (upstream control plane / etcd), not k3d as
   the default. Fast iteration: `kind load docker-image` + `imagePullPolicy: IfNotPresent`.
   Prod-like path: pull from GHCR — both documented; neither replaces the other.
5. **Topology default = kind single-node** through DS-019 (Temporal failover exit), DS-020
   (mesh under RAM budget), and DS-021 (SRE). Multi-node kind or kubeadm is an
   **ephemeral lab** (create → validate → destroy), scheduled post-DS-023 or folded into
   the consensus lab (DS-022) — never a permanent second control plane on the laptop.
6. **Roadmap split:** DS-018 implements `release.yml` + image contract + exit via
   `docker pull` of a tagged image. DS-019 starts only after that gate is green.
   Runbook: [`docs/runbooks/ghcr-release.md`](../runbooks/ghcr-release.md).
   Kind + Temporal worker failover (kustomize, not full mesh):
   [`docs/runbooks/kind-temporal-failover.md`](../runbooks/kind-temporal-failover.md),
   manifests under `platform/k8s/`. Diagrams: [`docs/diagrams/`](../diagrams/).
7. **RAM plan B:** if OOM/swap persists after documented tuning, offload to a remote
   cluster (see [`docs/OPEN_QUESTIONS.md`](../OPEN_QUESTIONS.md) Q18) — do not silently
   adopt multi-node on the laptop as the fix.
8. **Compose ↔ kind parity:** Service DNS names and app/infra images match Compose
   ([ADR-003](DS-ADR-003-config-server-and-dns-registry.md),
   [`platform/ports.md`](../../platform/ports.md)). Kind ConfigMaps project the same
   hostnames as `platform/config/*-compose.yml`.

## Consequences

- DS-018 exit is independently verifiable before any cluster work.
- No Docker Hub rate-limit pain for frequent PayHub pulls during DS-019+.
- Secrets surface stays minimal for PayHub image publish.
- Rejected: Docker Hub for PayHub images; k3d as the default local cluster; combining
  registry publish with K8s/GitOps in one ticket; permanent multi-node kind on a
  16 Go laptop as the learning default; kind-only hostnames or ad-hoc images that
  diverge from Compose.
