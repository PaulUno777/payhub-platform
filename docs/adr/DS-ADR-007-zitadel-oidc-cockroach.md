# ADR-007 — Zitadel OIDC + CockroachDB (IdP only)

- **Status:** Accepted
- **Date:** 2026-08-09
- **Ticket:** DS-011

## Context

DS-011 requires an OIDC issuer so the Gateway can reject calls without a valid JWT
before they reach business services (plan §17 #11, §10). PayHub services already
depend on `oauth2-resource-server` but run with permit-all `LocalSecurityConfig`.

Alternatives considered:

- **Keycloak on Postgres** — familiar Java IdP; would not diversify the local stack.
- **Cloud IdP only (Auth0 / Cognito)** — weaker local/offline DevContainer story.
- **Zitadel on Postgres** — supported by Zitadel; duplicates PayHub’s Postgres muscle
  memory and skips a deliberate non-Java datastore lab.

## Decision

1. **Zitadel** (Go) is the local OIDC IdP for PayHub Gateway, BFFs, and inbound
   resource servers. Issuer / JWKS are consumed via standard OIDC discovery.
2. **CockroachDB single-node** stores **Zitadel state only**. It is never a PayHub
   service database and never holds payment, merchant, or ledger data.
3. PayHub service databases remain **Postgres per service** (plan §3.1 / ADR-002).
4. JWT algorithms allowed: **RS256 / ES256** only; reject `alg=none` and symmetric
   algs for public clients (existing security rule).
5. Expected claims for tenant isolation: `sub` plus PayHub custom claim **`tenant_id`**
   (UUID string). Mapping from Zitadel org/project metadata is configured in the IdP;
   Gateway/services enforce claim match where a tenant is present on the request.
6. **FinLedger’s issuer stays separate** (`internal` sandbox / FinLedger’s own
   external IdP). PayHub does not replace FinLedger JWT minting.

## Consequences

- Compose profile `identity` + DevContainer bring up Zitadel + Cockroach for local
  edge work without expanding PayHub’s data plane.
- Rejected as default: Keycloak-on-Postgres for DS-011 learning goals; using Cockroach
  as a PayHub OLTP store; sharing FinLedger’s issuer for PayHub edge tokens.
- Production HA Cockroach / multi-region Zitadel is out of scope for DS-011.
