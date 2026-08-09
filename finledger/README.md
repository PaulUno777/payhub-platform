# FinLedger pin (external dependency — never a source checkout)

PayHub consumes FinLedger as an **unforked** dependency: Docker image + published
REST/OpenAPI contract only.

| Artifact | Value |
|----------|--------|
| Image | see [`IMAGE`](IMAGE) (`unoteck/finledger:<semver>`) |
| Pinned version | see [`PINNED_VERSION`](PINNED_VERSION) |
| Integration guide (copy) | [`INTEGRATION_GUIDE.md`](INTEGRATION_GUIDE.md) |
| OpenAPI path inventory (copy) | [`openapi-paths.json`](openapi-paths.json) |

Refresh these copies when bumping the pin — do not edit them to “fit” PayHub.
If PayHub needs a capability FinLedger does not expose, raise an ADR; never patch
the image.

## Local eval (PayHub Compose will wrap this)

```bash
# From a FinLedger clone (not this folder):
./bin/finledger-cli sandbox init --scenario aggregator
./bin/finledger-cli up --profile sandbox --build
```

Aggregator sandbox labels (FinLedger-owned):

| Role | FinLedger name | Stable tenant id |
|------|----------------|------------------|
| Aggregator | EcoPay Network | `…000a1` |
| Sub-merchant | **Send Tunnel** | `…000a2` |

Currency in that pack is **USD**. PayHub v1 uses the same currency for sandbox
parity (plan §0.2.1). PayHub’s mobile-money PSP stub is named **MmSandbox** — do
not confuse it with FinLedger’s “Send Tunnel” sub-merchant tenant label.

## Confirmed REST surface PayHub uses

From `openapi-paths.json` (FL-160):

- `POST /api/v1/tenants/{tenantId}/rails/payments` — initiate (PENDING)
- `POST /api/v1/tenants/{tenantId}/rails/payments/{railReference}/settle`
- `POST /api/v1/tenants/{tenantId}/splits`
- `POST /api/v1/tenants/{tenantId}/refunds`
- `PUT  /api/v1/tenants/{tenantId}/split-rules/{ruleSetKey}`
- `PUT  /api/v1/tenants/{tenantId}/fee-config`
- `POST /api/v1/tenants` — Merchant activation creates FinLedger `SUB_MERCHANT` (Q14 / DS-001)
- `POST /api/v1/tenants/{tenantId}/accounts` — wallets under that sub-merchant tenant

Do **not** point MmSandbox webhooks at
`…/rails/webhooks/settlement` (FinLedger HMAC). Orchestrator settles via JWT
`…/settle` after real PSP proof (plan §4.1, §9.1).
