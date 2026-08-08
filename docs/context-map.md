# Context map — EcoPay / PayHub (DS-001)

Status: **draft** — exit criterion DS-001: every bounded context has an owner; map
reviewed and approved.

## Bounded contexts (PayHub)

| Context | Service | Aggregate | Upstream / downstream |
|---------|---------|-----------|------------------------|
| Merchant | `merchant-service` | `Merchant` | Downstream of Ops BFF (approve); upstream of Orchestrator (`assignedRuleSetKey`, FinLedger account/tenant refs) |
| Payment Orchestrator | `payment-orchestrator` | `Payment` (+ refund states) | Downstream of Merchant, Risk, Rail; sole caller of FinLedger payment APIs |
| Risk | `risk-service` | `RiskAssessment` | Upstream of Orchestrator (sync decision) |
| Rail Adapter | `rail-adapter-service` | `RailOperation` | Downstream of Orchestrator; **PSP MmSandbox only** — never FinLedger |
| Reconciliation | `reconciliation-service` | `ReconciliationRun`, `Break` | Choreography on Kafka + ops actions; may *command* Orchestrator/FinLedger via audited ops — never SQL |
| Reporting | `reporting-service` | `PaymentView` | Kafka projections; non-authoritative |
| Notification | `notification-service` | `WebhookDelivery` | Consumes `payment.lifecycle.v1` only |
| Merchant BFF / Ops BFF | `merchant-bff`, `ops-bff` | — | Composition only; no business truth |
| Edge | `gateway` | — | TLS, JWT, rate limit; no payment rules |

## External system

| Context | Role |
|---------|------|
| **FinLedger** | Monetary source of truth. REST: accounts, rails initiate/settle, splits, refunds, fee-config, split-rules. Outbox → CDC → `ledger.journal-entry.v1`. |
| **MmSandbox** | PayHub-owned PSP stub (not a FinLedger component). |

## FinLedger naming (do not collide)

| FinLedger sandbox label | PayHub meaning |
|-------------------------|----------------|
| EcoPay Network (`…a1`) | Aggregator tenant |
| Send Tunnel (`…a2`) | **Sub-merchant tenant** in FinLedger seed |
| *(n/a)* | PayHub PSP stub = **MmSandbox** |

## Relationship styles

```text
[Ops BFF] --OHS--> [Merchant] --ACL--> [FinLedger accounts/tenants]
[Merchant BFF] --OHS--> [Orchestrator] --ACL--> [FinLedger rails/splits/refunds]
[Orchestrator] --OHS--> [Risk]
[Orchestrator] --OHS--> [Rail Adapter] --ACL--> [MmSandbox]
[Orchestrator] --Pub--> payment.lifecycle.v1 --> [Reporting|Notification|Reconciliation]
[Rail Adapter] --Pub--> rail.operation.v1 --> [Orchestrator|Reconciliation]
[FinLedger outbox] --CDC--> ledger.journal-entry.v1 --> [Reporting|Reconciliation]
```

- **OHS** = Open Host Service (stable HTTP API inside PayHub)
- **ACL** = Anti-Corruption Layer (`LedgerPort`, `AccountProvisioningPort`, `RailProviderPort`)
- **Pub** = published language via versioned Kafka topics

## Open mapping decision (Q14)

Prefer: active `Merchant` → FinLedger `SUB_MERCHANT` tenant + wallets (matches aggregator
sandbox). Alternative: accounts under EcoPay tenant only — weaker JWT/`tenant_id` story.

## CAP/PACELC

See [adr/DS-ADR-001-cap-pacelc.md](adr/DS-ADR-001-cap-pacelc.md) and plan §3.3.
