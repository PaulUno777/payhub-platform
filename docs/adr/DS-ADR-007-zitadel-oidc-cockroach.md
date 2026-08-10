# ADR-007 — Zitadel OIDC + IdP datastore (IdP only)

- **Status:** Amended
- **Date:** 2026-08-09
- **Amended:** 2026-08-10
- **Ticket:** DS-011 (decision); amendment for laptop/kind RAM (roadmap DS-019+)

## Context

DS-011 requires an OIDC issuer so the Gateway can reject calls without a valid JWT
before they reach business services (plan §17 #11, §10). PayHub services already
depend on `oauth2-resource-server` but run with permit-all `LocalSecurityConfig`.

The original decision used **CockroachDB single-node** for Zitadel state to diversify
the local stack (non-Java datastore lab). On a Mac Intel 16 Go laptop with Docker
Desktop capped near ~8 Go for the Linux VM, that IdP footprint (often 1.5 Go+) competes
with the DS-020 kind mesh (Postgres-per-service, Kafka, Temporal, Spring apps). Zitadel
natively supports Postgres for preview/dev.

Alternatives considered:

- **Keycloak on Postgres** — familiar Java IdP; would not diversify the local stack.
- **Cloud IdP only (Auth0 / Cognito)** — weaker local/offline DevContainer story.
- **Zitadel on Cockroach always** — keeps diversity lab; burns RAM needed for the mesh.
- **Zitadel on Postgres (lab default)** — supported by Zitadel; lower RAM; IdP state still
  isolated from PayHub service databases.

## Decision

1. **Zitadel** (Go) is the local OIDC IdP for PayHub Gateway, BFFs, and inbound
   resource servers. Issuer / JWKS are consumed via standard OIDC discovery.
2. **Lab / constrained default (Compose kind-mesh, DS-020 footprint):** Zitadel state
   lives on a **dedicated Postgres** instance (`postgres-zitadel`). It is never a PayHub
   service database and never holds payment, merchant, or ledger data.
3. **Optional diversity profile `identity-crdb`:** **CockroachDB single-node** remains
   available when RAM allows, for the original non-Java datastore lab. Same isolation
   rule — IdP only, never PayHub OLTP.
4. PayHub service databases remain **Postgres per service** (plan §3.1 / ADR-002).
5. JWT algorithms allowed: **RS256 / ES256** only; reject `alg=none` and symmetric
   algs for public clients (existing security rule).
6. Expected claims for tenant isolation: `sub` plus PayHub custom claim **`tenant_id`**
   (UUID string). Mapping from Zitadel org/project metadata is configured in the IdP;
   Gateway/services enforce claim match where a tenant is present on the request.
7. **FinLedger’s issuer stays separate** (`internal` sandbox / FinLedger’s own
   external IdP). PayHub does not replace FinLedger JWT minting.

## Consequences

- Compose / kind docs target Zitadel + `postgres-zitadel` for the default laptop path;
  Cockroach is opt-in, not the always-on IdP store.
- Rejected as default: Keycloak-on-Postgres for DS-011 learning goals; using Cockroach
  (or the IdP Postgres) as a PayHub OLTP store; sharing FinLedger’s issuer for PayHub
  edge tokens; requiring Cockroach on every constrained laptop run.
- Production HA Cockroach / multi-region Zitadel remains out of scope for DS-011.
- Implementation of the Postgres IdP default (Compose profile switch) follows the
  amended ADR in a later ticket (DS-020 mesh / identity follow-up) — this amendment
  is the decision record first.
