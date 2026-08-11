# Identity stack (DS-011 / ADR-007)

Local OIDC for PayHub edge: **Zitadel** + **CockroachDB** (IdP state only).

## Start

```bash
cd platform/compose
docker compose --profile identity up -d cockroachdb zitadel
```

Wait until Zitadel is healthy, then open:

| URL                                                      | Purpose                         |
| -------------------------------------------------------- | ------------------------------- |
| <http://localhost:8090/ui/console>                       | Zitadel console                 |
| <http://localhost:8090/.well-known/openid-configuration> | OIDC discovery                  |
| <http://localhost:8086>                                  | Cockroach DB Console (optional) |

Default first-instance admin (local only — change if exposed):

- user: `zitadel-admin`
- password: `PayHubAdmin1!`

Master key env: `ZITADEL_MASTERKEY` (must be ≥32 chars; compose default is for local lab).

## Seed OIDC application (Gateway API)

In the Zitadel console:

1. Create a project `payhub`.
2. Create an **API** application (JWT access tokens) for the Gateway / resource servers.
3. Create a **user** for Ops demo; set user metadata / custom claim **`tenant_id`** to a
   PayHub tenant UUID (see OPEN_QUESTIONS Q17).
4. Note the issuer (`http://localhost:8090`) and set for Gateway / BFFs:

```yaml
payhub:
  security:
    issuer-uri: http://localhost:8090
```

(or env `PAYHUB_OIDC_ISSUER`).

## Boundaries

- CockroachDB is **never** used by PayHub services (Postgres per service remains).
- FinLedger keeps `FINLEDGER_SECURITY_ISSUER=internal` in sandbox — separate from PayHub edge JWT.
