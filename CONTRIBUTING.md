# Contributing to PayHub

Thanks for helping build a correct, learning-grade distributed payments platform around
FinLedger.

## Mission check

PayHub simulates aggregator **EcoPay Network**. It is **not** a Stripe/Adyen rebuild,
**not** a second ledger, and **not** a FinLedger fork or SPI host. Prefer depth on
idempotency, sagas, ambiguous settlement, outbox/inbox, and resilience over feature
breadth.

## How to contribute (fork → PR)

**Do not ask for write access** to this repository. Only the maintainer merges into
`develop` and `main`.

1. **Fork** the repo and clone your fork.
2. Branch from up-to-date `develop`: `ds-0xx/short-slug`.
3. Open a **pull request targeting `develop`** (never open feature PRs into `main`).
4. Wait for CI (`Maven test`). Fix failures on your branch.
5. The maintainer reviews and merges.

Release flow (maintainer only): PR `develop` → `main`, then tag `v*.*.*` when publishing.

## Branch model

| Branch | Role |
|--------|------|
| `main` | Releases only — no direct feature commits |
| `develop` | Integration base for all new work |
| `ds-0xx/short-slug` | One roadmap ticket from plan §17 |

## Phase gates

Work proceeds **one DS-0xx ticket at a time** (see [docs/development.md](docs/development.md)).
After a ticket PR merges to `develop`, wait for confirmation before starting the next.

Before declaring a ticket done:

1. Unit / architecture tests for the change
2. ArchUnit still green (`domain` stays framework-free)
3. Integration tests when the ticket touches persistence or messaging
4. The ticket's exit criterion from `docs/PLAN_PAYHUB.md` §17 holds

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:`, `fix:`, `chore:`, `test:`, `docs:`, `ci:`
- One commit = one atomic concern

## Local setup

```bash
docker compose -f platform/compose/docker-compose.yml up -d
./mvnw -B test
./mvnw -pl services/payment-orchestrator test
```

## Architecture rules

See `.cursor/rules/architecture.mdc` and `docs/architecture.md`. No shared domain module;
no FinLedger client outside each service's own port/ACL.
