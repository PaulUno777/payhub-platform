# Context map — EcoPay / PayHub (DS-001)

Status: **ready for review** (DS-001 exit: every bounded context has an owner; CAP ADR
accepted; Q14 locked). Human approval via PR merge into `develop`.

## Bounded contexts (PayHub) — owners

| Context | Service (owner) | Aggregate | Upstream / downstream |
|---------|-----------------|-----------|------------------------|
| Merchant | `merchant-service` | `Merchant` | Downstream of Ops BFF (approve); upstream of Orchestrator (`assignedRuleSetKey`, `finLedgerTenantId` + wallet refs). Provisions FinLedger `SUB_MERCHANT` tenant + wallets on activation. |
| Payment Orchestrator | `payment-orchestrator` | `Payment` (+ refund states) | Downstream of Merchant, Risk, Rail; **sole** caller of FinLedger payment APIs (`LedgerPort`) |
| Risk | `risk-service` | `RiskAssessment` | Upstream of Orchestrator (sync decision) |
| Rail Adapter | `rail-adapter-service` | `RailOperation` | Downstream of Orchestrator; **PSP MmSandbox only** — never FinLedger |
| Reconciliation | `reconciliation-service` | `ReconciliationRun`, `Break` | Choreography on Kafka + ops actions; audited commands only — never SQL on FinLedger |
| Reporting | `reporting-service` | `PaymentView` | Kafka projections; non-authoritative |
| Notification | `notification-service` | `WebhookDelivery` | Consumes `payment.lifecycle.v1` only |
| Merchant BFF | `merchant-bff` | — | Composition only; no business truth |
| Ops BFF | `ops-bff` | — | Composition only; no business truth |
| Edge | `gateway` | — | TLS, JWT, rate limit; no payment rules |

## External system

| Context | Role |
|---------|------|
| **FinLedger** | Monetary source of truth. REST: tenants, accounts, rails initiate/settle, splits, refunds, fee-config, split-rules. Outbox → CDC → `ledger.journal-entry.v1`. Has its **own** in-box reconciliation (rail_instruction vs settlement report) — distinct from PayHub Reconciliation. |
| **MmSandbox** | PayHub-owned PSP stub (not a FinLedger component). |

## Two reconciliations (do not conflate)

| System | Scope |
|--------|--------|
| **FinLedger** reconciliation | Matches FinLedger `rail_instruction` lines to ingested settlement reports inside FinLedger (ADR-009). |
| **PayHub** Reconciliation | EcoPay ops: PSP statements vs PayHub `RailOperation` / payment state vs ledger evidence; produces PayHub `Break`s and ops actions (`request reversal`, etc.). |

## FinLedger naming (do not collide)

| FinLedger sandbox label | PayHub meaning |
|-------------------------|----------------|
| EcoPay Network (`…a1`) | Aggregator tenant |
| Send Tunnel (`…a2`) | **Sub-merchant tenant** in FinLedger seed |
| *(n/a)* | PayHub PSP stub = **MmSandbox** |

## Relationship styles

```text
[Ops BFF] --OHS--> [Merchant] --ACL--> [FinLedger SUB_MERCHANT tenant + wallets]
[Merchant BFF] --OHS--> [Orchestrator] --ACL--> [FinLedger rails/splits/refunds]
[Orchestrator] --OHS--> [Risk]
[Orchestrator] --OHS--> [Rail Adapter] --ACL--> [MmSandbox]
[Orchestrator] --Pub--> payment.lifecycle.v1 --> [Reporting|Notification|Reconciliation]
[Rail Adapter] --Pub--> rail.operation.v1 --> [Orchestrator|Reconciliation]
[FinLedger outbox] --CDC--> ledger.journal-entry.v1 --> [Reporting|Reconciliation]
```

- **OHS** = Open Host Service (stable HTTP API inside PayHub)
- **ACL** = Anti-Corruption Layer (`LedgerPort`, `AccountProvisioningPort` = tenant+wallets, `RailProviderPort`)
- **Pub** = published language via versioned Kafka topics

## Q14 — locked (DS-001)

Active `Merchant` → FinLedger **`SUB_MERCHANT` tenant + wallets** (matches aggregator
sandbox). Not “account under EcoPay tenant only.” See plan §9.3 / §19.

## CAP/PACELC

See [adr/DS-ADR-001-cap-pacelc.md](adr/DS-ADR-001-cap-pacelc.md) (**Accepted**) and plan §3.3.
