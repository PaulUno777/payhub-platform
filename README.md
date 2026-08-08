# PayHub Platform

[![CI](https://github.com/PaulUno777/payhub-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/PaulUno777/payhub-platform/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green.svg)](https://spring.io/projects/spring-boot)

Learning-grade **distributed payments platform** simulating aggregator **EcoPay Network**,
built **around** [FinLedger](https://github.com/PaulUno777/finledger) — never forking it,
never growing a second ledger.

PayHub owns payment lifecycle, risk, real PSP conversation, merchant onboarding,
reconciliation ops, CQRS reads, and merchant webhooks. FinLedger owns monetary truth
(journals, splits, refunds, rail PENDING→SETTLED accounting).

## What this is / is not

| Is | Is not |
|----|--------|
| Depth on idempotency, sagas, ambiguity → reconciliation, outbox/inbox, resilience | A Stripe/Adyen clone |
| 10 Spring Boot services from day one (plan §0.2 / §19) | A FinLedger fork or SPI host |
| EcoPay sandbox journey: risk → MmSandbox PSP → FinLedger settle/split → refund | Multi-region / ML fraud / day-1 service mesh |

## Sources of truth

| Doc | Role |
|-----|------|
| [`docs/PLAN_PAYHUB.md`](docs/PLAN_PAYHUB.md) | Product + architecture plan (normative) |
| [`docs/architecture.md`](docs/architecture.md) | Hexagonal layout + ownership |
| [`docs/development.md`](docs/development.md) | Ticket map DS-001…024, bootstrap |
| [`docs/OPEN_QUESTIONS.md`](docs/OPEN_QUESTIONS.md) | Living assumptions register |
| [`finledger/`](finledger/) | Pinned FinLedger image + contract copies |
| [`.cursor/rules/`](.cursor/rules/) | Agent/engineering gates |

## Topology (short)

```text
Gateway → BFFs → Payment Orchestrator (Temporal)
                    ├─ Risk / Merchant / Rail Adapter (MmSandbox PSP only)
                    └─ LedgerPort → FinLedger (rails / settle / splits / refunds)

Kafka: payment.lifecycle · rail.operation · ledger.journal-entry (CDC)
Notification ← payment.lifecycle only (not FinLedger outbox)
```

**Order locked for capture:** PSP first → FinLedger `initiate` (PENDING) → final proof →
`settle` → `splits`. Rail Adapter never calls FinLedger.

## Services

Hexagonal Spring Boot **4.1.0** modules under [`services/`](services/) (ArchUnit per service):

`gateway`, `merchant-bff`, `ops-bff`, `merchant-service`, `payment-orchestrator`,
`risk-service`, `rail-adapter-service`, `reconciliation-service`, `reporting-service`,
`notification-service`

## Naming (avoid confusion with FinLedger sandbox)

| Name | Meaning |
|------|---------|
| **EcoPay Network** | Aggregator (demo) — also FinLedger aggregator tenant label |
| **Send Tunnel** | FinLedger **sub-merchant** sandbox tenant — **not** PayHub’s PSP |
| **MmSandbox** | PayHub’s mobile-money **PSP stub** (Rail Adapter) |
| **USD** | v1 single currency (matches FinLedger `aggregator` pack) |

## Quick start

```bash
docker compose -f platform/compose/docker-compose.yml up -d
./mvnw -B test
```

## Roadmap

Work proceeds **one DS-0xx ticket at a time** from `develop`
(see [`docs/development.md`](docs/development.md)):

1. **DS-001** — DDD context map + CAP/PACELC ADR (done)
2. **DS-002** — Hexagonal skeletons, ArchUnit, Compose, CI (in progress)
3. **DS-003** — FinLedger `LedgerPort` integration  
… through **DS-024** capstone.

## Quick links

- Plan §17 exit criteria: [`docs/PLAN_PAYHUB.md`](docs/PLAN_PAYHUB.md)
- Contributing: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- FinLedger integration copy: [`finledger/INTEGRATION_GUIDE.md`](finledger/INTEGRATION_GUIDE.md)
- OpenAPI paths PayHub must respect: [`finledger/openapi-paths.json`](finledger/openapi-paths.json)
