# ADR-008 — PayHub images on GHCR; local cluster = kind

- **Status:** Accepted
- **Date:** 2026-08-09
- **Ticket:** DS-018 (registry); informs DS-019 (Kubernetes)

## Context

`project-workflow.mdc` promised a `release.yml` that builds and pushes multi-arch images,
but only `ci.yml` (Maven test) existed. DS-019 (Kubernetes/GitOps, formerly DS-018) cannot
claim a prod-like cluster if image publish is untested folklore. Folding registry + kind +
Temporal failover into one PR violates the one-concern rule (architecture §9 / DS-016
lesson). Docker Hub vs GHCR and kind vs k3d needed an explicit choice.

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
   the default. Reasons: closer to “real” Kubernetes for later data-safety and consensus
   observation (DS-020 / DS-022). Fast iteration: `kind load docker-image` +
   `imagePullPolicy: IfNotPresent`. Prod-like path: pull from GHCR — both documented;
   neither replaces the other.
5. **Roadmap split:** DS-018 implements `release.yml` + image contract + exit via
   `docker pull` of a tagged image. DS-019 starts only after that gate is green.

## Consequences

- DS-018 exit is independently verifiable before any cluster work.
- No Docker Hub rate-limit pain for frequent PayHub pulls during DS-019+.
- Secrets surface stays minimal for PayHub image publish.
- Rejected: Docker Hub for PayHub images; k3d as the default local cluster; combining
  registry publish with K8s/GitOps in one ticket.
