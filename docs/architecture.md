# Architecture overview

PayHub is a **multi-service** platform: each bounded context (plan §1.2) is its own
Spring Boot module — **10 services from day one** (plan §0.2, §19). Each service keeps the
same **hexagonal (ports & adapters)** shape FinLedger uses internally.

```text
Adapters in → Application (ports/use cases) → Domain (framework-free)
                      ↑
              Infrastructure implements ports
              · Orchestrator FinLedgerClient (LedgerPort): rails/settle/splits/refunds
              · Merchant FinLedger client (AccountProvisioningPort): accounts only
              · Rail Adapter: PSP only — NO FinLedger client
```

## Topology (ownership)

```text
Gateway → Merchant BFF | Payment Orchestrator | Ops BFF
                         |
         +---------------+---------------+
         |               |               |
   Merchant Service  Risk Service   Rail Adapter
   (account provis.                 (PSP MmSandbox
    → FinLedger)                     ONLY — no FinLedger)
                         |
         Orchestrator LedgerPort → FinLedger
           (rails / settle / splits / refunds)

Kafka:
  payment.lifecycle.v1  (Orchestrator → Reporting, Notification, Reconciliation)
  rail.operation.v1     (Rail Adapter → Orchestrator, Reconciliation)
  ledger.journal-entry.v1 (FinLedger CDC → Reporting, Reconciliation)

Notification consumes payment.lifecycle.v1 only (never FinLedger outbox directly).
```

**Ownership (non-negotiable):** Rail Adapter never calls FinLedger. Orchestrator alone
drives payment/refund ledger calls, and only **after** the PSP has accepted processing
(plan §4.1). Merchant keeps a separate FinLedger ACL for account provisioning — a second
ACL, not a contradiction.

## Repo layout

```text
payhub-platform/
├── pom.xml
├── build-conventions/
├── services/
│   ├── gateway/
│   ├── merchant-bff/
│   ├── ops-bff/
│   ├── merchant-service/
│   ├── payment-orchestrator/     # LedgerPort + RailPort + Temporal
│   ├── risk-service/
│   ├── rail-adapter-service/      # PSP only — no FinLedger dependency
│   ├── reconciliation-service/
│   ├── reporting-service/
│   └── notification-service/
├── contracts/
├── platform/{compose,kafka,k8s,observability,chaos}/
├── finledger/                   # pinned image + INTEGRATION_GUIDE.md copy
└── docs/
    ├── PLAN_PAYHUB.md
    ├── architecture.md
    ├── development.md
    ├── OPEN_QUESTIONS.md
    ├── context-map.md
    ├── event-catalog.md
    ├── adr/
    └── runbooks/
```

## Non-negotiables

- Idempotency for every mutating inbound request and every message consumer
- Transactional outbox — never a dual write
- No 2PC/XA — Temporal sagas with explicit compensation only
- Ambiguous rail state → `RECONCILIATION_REQUIRED` / `REFUND_RECONCILIATION_REQUIRED`
- Each service owns its own database
- FinLedger is the sole monetary source of truth; PayHub never recomputes split % or
  fee-reversal policy — only selects `ruleSetKey` and calls `/splits` / `/refunds`
- Per-service FinLedger ACL only (`LedgerPort` on Orchestrator; `AccountProvisioningPort`
  on Merchant). Controllers, consumers, Temporal activities, and Rail Adapter never call
  FinLedger REST directly
- Payment order: PSP first → `initiate` (PENDING) → final proof → `settle` (plan §4.1)
- Tenant isolation checked in every service

## Where to read next

| Topic | Document |
|-------|----------|
| Full product plan | [PLAN_PAYHUB.md](PLAN_PAYHUB.md) |
| Structural decisions | [PLAN_PAYHUB.md §19](PLAN_PAYHUB.md) and [adr/](adr/) |
| Payment + refund state machines | Plan §1.4, §4.1, §4.3 |
| Ports per service | Plan §2.2 |
| Roadmap / bootstrap | [development.md](development.md) |
| FinLedger contract (external) | `finledger/INTEGRATION_GUIDE.md` |
| Open questions | [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md) |
