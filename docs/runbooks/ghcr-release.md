# GHCR release (DS-018)

PayHub service images publish to **GitHub Container Registry** via
[`.github/workflows/release.yml`](../../.github/workflows/release.yml) (ADR-008).
FinLedger stays on its pinned Docker Hub image — not this path.

## Image names

```text
ghcr.io/pauluno777/payhub-<service>:<tag>
```

Services: `config-server`, `gateway`, `merchant-bff`, `merchant-service`,
`notification-service`, `ops-bff`, `payment-orchestrator`, `rail-adapter-service`,
`reconciliation-service`, `reporting-service`, `risk-service`.

### Tag contract

| Source                           | Tags pushed                                                              |
| -------------------------------- | ------------------------------------------------------------------------ |
| Git tag `vX.Y.Z` (e.g. `v0.1.0`) | `:X.Y.Z` and `:latest` (semver always present — never `:latest` alone)   |
| Push to `main` (no tag)          | `:sha-<7char>` and `:main`                                               |
| Laptop / Compose                 | `pauluno/payhub-<service>:local` via repo-root `docker build` (not GHCR) |

Auth in Actions: `GITHUB_TOKEN` with `packages: write` only — no Docker Hub token for PayHub.

## How to cut a release

1. Merge the release candidate to `main` (human release PR from `develop`).
2. Tag and push:

```bash
git checkout main && git pull
git tag v0.1.0
git push origin v0.1.0
```

1. Wait for the **Release** workflow on that tag (test → multi-arch publish → GitHub Release).
2. Packages appear under the repo **Packages** tab (`payhub-<service>`).

### Package visibility (anonymous pull)

New GHCR packages default to private. For the DS-018 exit check without a token, set each
package (or the org default) to **Public** once in the GitHub UI, **or** pull while logged in:

```bash
echo "$GITHUB_TOKEN" | docker login ghcr.io -u USERNAME --password-stdin
```

## Exit criterion checklist (DS-018)

- [x] Tag `v0.1.0` pushed to `origin`
- [x] Actions workflow **Release** green for that tag
- [x] At least one image pullable outside CI:

```bash
docker pull ghcr.io/pauluno777/payhub-payment-orchestrator:0.1.0
```

DS-018 exit confirmed. Do **not** start DS-019 (kind / GitOps) until you explicitly open that ticket.

## Local build (unchanged)

```bash
docker build -f services/payment-orchestrator/Dockerfile \
  -t pauluno/payhub-payment-orchestrator:local .
```

For kind iteration later (DS-019): `kind load docker-image …` + `imagePullPolicy: IfNotPresent`,
or pull the GHCR semver tag for the prod-like path.
