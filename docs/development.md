# Development guide

## Sources of truth

- Product / architecture plan: [PLAN_PAYHUB.md](PLAN_PAYHUB.md) (roadmap §17)
- FinLedger's contract (external, read-only): `finledger/INTEGRATION_GUIDE.md`
- ADRs: [adr/](adr/)
- Open questions / assumptions register: [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md)
- Engineering rules: `.cursor/rules/architecture.mdc`
- Workflow gates: `.cursor/rules/project-workflow.mdc`

## Branch model

- `main` — releases only
- `develop` — integration base
- `ds-0xx/short-slug` — one ticket from `develop`

PRs are human-owned (agents do not open them unless asked).

## Ticket / branch map (plan §17 — renumbered after the design review)

| Ticket | Phase | Branch slug | Status |
|--------|-------|-------------|--------|
| DS-001 | DDD cadrage: event storming, context map (incl. Merchant), ubiquitous language, ownership, CAP/PACELC ADR | `ds-001/context-map-adr` | done |
| DS-002 | Repo foundation: hexagonal skeleton for all 10 services, ArchUnit, Compose, CI, contract conventions | `ds-002/repo-foundation` | done |
| DS-003 | FinLedger integration: pinned image, Orchestrator `LedgerPort` ACL (smoke rails/connectivity), tenant/trace/idempotency | `ds-003/finledger-integration` | done |
| DS-004 | Merchant service: `Merchant` aggregate, FinLedger `SUB_MERCHANT` tenant + wallets on activation, Ops BFF approve/reject | `ds-004/merchant-service` | done |
| DS-005 | Event backbone: FinLedger outbox → Debezium → Kafka, Schema Registry, AsyncAPI, first inbox consumer | `ds-005/outbox-debezium-kafka` | done |
| DS-006 | Payment happy path (no real rail): `Payment` aggregate, API idempotency, Temporal, sync = `RISK_APPROVED`, RailPort stub | `ds-006/payment-happy-path` | done |
| DS-007 | Processing guarantees: standardized inbox/outbox, retries, idempotent side effects | `ds-007/processing-guarantees` | done |
| DS-008 | Rail + compensation: PSP→initiate→settle order, MmSandbox, ambiguous timeout; happy path to `SETTLED` | `ds-008/rail-compensation` | done |
| DS-009 | Refund: `RefundWorkflow`, `POST .../refunds`, `NO_REVERSE` tenant policy provisioned | `ds-009/refund-workflow` | in progress |
| DS-010 | Reconciliation: statement import, breaks, lock/leadership, minimal Ops console | `ds-010/reconciliation` | pending |
| DS-011 | Edge: Gateway, OIDC/JWT, tenant isolation, rate limiting, Merchant/Ops BFF | `ds-011/edge-gateway-bff` | pending |
| DS-012 | Tracing: end-to-end OTel, trace linked across events and workflows | `ds-012/otel-tracing` | pending |
| DS-013 | CQRS: Reporting projection, staleness, Redis cache-aside, event-driven invalidation | `ds-013/cqrs-reporting` | pending |
| DS-014 | Notifications: signed webhooks, retries, DLQ; RabbitMQ POC documented if useful | `ds-014/notifications-webhooks` | pending |
| DS-015 | Resilience: budgets, timeouts, retries, circuit breakers, bulkheads, shedding, backpressure | `ds-015/resilience` | pending |
| DS-016 | Event operations: retry topics, replay tool, quotas, rebalances, poison-message procedure | `ds-016/event-operations` | pending |
| DS-017 | Chaos/load: fault injection, blast-radius measurement, capacity report | `ds-017/chaos-load` | pending |
| DS-018 | Kubernetes/GitOps: Services/DNS, policies, HPA/KEDA, PDB, secrets, progressive delivery | `ds-018/k8s-gitops` | pending |
| DS-019 | Data safety: HA DB/Kafka, PITR, restore test, CDC recovery, expand/contract migrations | `ds-019/data-safety` | pending |
| DS-020 | SRE: SLOs/error budgets, alerts, runbooks, postmortem template | `ds-020/sre-slo` | pending |
| DS-021 | Consensus lab: etcd/KRaft, leader failure, Lease and fencing-token exercise | `ds-021/consensus-lab` | pending |
| DS-022 | DR game day: simulated zone loss, recovery within RPO/RTO, reconciliation, report | `ds-022/dr-game-day` | pending |
| DS-023 | Mesh POC: only after a cost/benefit ADR; mTLS and canary compared to the in-app solution | `ds-023/mesh-poc` | pending |
| DS-024 | Capstone: payment + refund demo with rail/Kafka outage, recovery, reconciliation, audit, architecture review | `ds-024/capstone` | pending |

Work proceeds **one ticket at a time** with human PR gates. Check the ticket's exit
criterion from `PLAN_PAYHUB.md` §17 explicitly before considering it done.

## Phase gate checklist

1. Relevant tests green (unit, integration via Testcontainers, contract where applicable)
2. ArchUnit green for every touched service
3. The ticket's stated exit criterion (plan §17) holds — verify explicitly
4. Atomic Conventional Commit(s) on the feature branch
5. Human merges PR into `develop`
6. Only then start the next ticket

---

## Bootstrapping the base project (DS-002 / DS-003)

PayHub is **10 independently-releasable services** plus `config-server` (Spring Cloud Config)
and a shared `build-conventions` module. Port map: [`platform/ports.md`](../platform/ports.md).
Config YAML (profiles `local` / `compose`): [`platform/config/`](../platform/config/).

Generate each business service from [start.spring.io](https://start.spring.io) individually,
with only the starters that service actually needs **at scaffold time** — Kafka, Temporal, Redis,
and AMQP are added later, only in the ticket that actually introduces them (see the note
after the commands). Don't add them up front; that was a mistake in an earlier draft of
this guide and it contradicts the project's own progressive-complexity principle
(plan §0.2.8).

Config Server must be reachable for `compose` profile (or use `optional:configserver:` +
classpath test YAML). Boot order locally: `config-server` → Postgres → apps / FinLedger.
### 0. Conventions used for every service

- `groupId`: `com.payhub`
- `packageName`: `com.payhub.<service>`
- `javaVersion`: `21`
- `packaging`: `jar`
- `bootVersion`: **`4.1.0`** (pass `-d bootVersion=4.1.0.RELEASE` to Initializr; reactor
  parent pins `spring-boot-starter-parent` **4.1.0** so all services stay in lockstep)

### 1. Generate each service via the Initializr HTTP API — DS-002 minimal starters only

```bash
mkdir -p services

# --- gateway --------------------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=gateway -d name=gateway \
  -d packageName=com.payhub.gateway \
  -d dependencies=cloud-gateway,actuator,oauth2-resource-server \
  -o gateway.zip
unzip -q gateway.zip -d services/gateway && rm gateway.zip

# --- merchant-bff ----------------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=merchant-bff -d name=merchant-bff \
  -d packageName=com.payhub.merchantbff \
  -d dependencies=web,validation,actuator,oauth2-resource-server \
  -o merchant-bff.zip
unzip -q merchant-bff.zip -d services/merchant-bff && rm merchant-bff.zip

# --- ops-bff -----------------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=ops-bff -d name=ops-bff \
  -d packageName=com.payhub.opsbff \
  -d dependencies=web,validation,actuator,oauth2-resource-server \
  -o ops-bff.zip
unzip -q ops-bff.zip -d services/ops-bff && rm ops-bff.zip

# --- merchant-service --------------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=merchant-service -d name=merchant-service \
  -d packageName=com.payhub.merchant \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,actuator,oauth2-resource-server,testcontainers \
  -o merchant-service.zip
unzip -q merchant-service.zip -d services/merchant-service && rm merchant-service.zip

# --- payment-orchestrator ---------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=payment-orchestrator -d name=payment-orchestrator \
  -d packageName=com.payhub.orchestrator \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,actuator,oauth2-resource-server,testcontainers \
  -o payment-orchestrator.zip
unzip -q payment-orchestrator.zip -d services/payment-orchestrator && rm payment-orchestrator.zip
# Kafka added at DS-005, Temporal SDK (no official starter) added at DS-006:
#   io.temporal:temporal-sdk, io.temporal:temporal-testing (test scope)

# --- risk-service --------------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=risk-service -d name=risk-service \
  -d packageName=com.payhub.risk \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,actuator,testcontainers \
  -o risk-service.zip
unzip -q risk-service.zip -d services/risk-service && rm risk-service.zip

# --- rail-adapter-service --------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=rail-adapter-service -d name=rail-adapter-service \
  -d packageName=com.payhub.railadapter \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,actuator,testcontainers \
  -o rail-adapter-service.zip
unzip -q rail-adapter-service.zip -d services/rail-adapter-service && rm rail-adapter-service.zip
# Kafka added at DS-005. Resilience4j (no official starter) added at DS-015:
#   io.github.resilience4j:resilience4j-spring-boot3

# --- reconciliation-service -------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=reconciliation-service -d name=reconciliation-service \
  -d packageName=com.payhub.reconciliation \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,actuator,testcontainers \
  -o reconciliation-service.zip
unzip -q reconciliation-service.zip -d services/reconciliation-service && rm reconciliation-service.zip
# Kafka added at DS-005

# --- reporting-service -------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=reporting-service -d name=reporting-service \
  -d packageName=com.payhub.reporting \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,actuator,testcontainers \
  -o reporting-service.zip
unzip -q reporting-service.zip -d services/reporting-service && rm reporting-service.zip
# Kafka added at DS-005, Redis (data-redis) added at DS-013

# --- notification-service ------------------------------------------------
curl https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d javaVersion=21 \
  -d groupId=com.payhub -d artifactId=notification-service -d name=notification-service \
  -d packageName=com.payhub.notification \
  -d dependencies=web,data-jpa,postgresql,flyway,validation,actuator,testcontainers \
  -o notification-service.zip
unzip -q notification-service.zip -d services/notification-service && rm notification-service.zip
# AMQP (RabbitMQ) added at DS-014
```

Dependency timing, made explicit so nothing gets added early "just in case":

| Dependency | Services | Added at |
|---|---|---|
| `kafka` (Spring Kafka) | payment-orchestrator, rail-adapter-service, reconciliation-service, reporting-service | DS-005 (event backbone) |
| Temporal SDK | payment-orchestrator | DS-006 (payment happy path) |
| `data-redis` | reporting-service | DS-013 (CQRS) |
| `amqp` (Spring AMQP) | notification-service | DS-014 (notifications) |
| Resilience4j | rail-adapter-service (+ others as needed) | DS-015 (resilience) |

- `flyway` everywhere a service owns a Postgres schema — migrations are expand/contract
  from day one (plan §15.2).
- `oauth2-resource-server` on every service that terminates an inbound JWT.
- `testcontainers` on every service with a database dependency — no H2/embedded
  substitutes, per `architecture.mdc` §8.

### 2. Add what Initializr doesn't ship (every service, right after unzip)

1. Add ArchUnit (`com.tngtech.archunit:archunit-junit5`, test scope) and the domain-purity
   rule from `architecture.md` — copy from `build-conventions/` once it exists.
2. Rename the generated main class to `<Service>Application`.
3. Restructure into `domain/`, `application/`, `infrastructure/`, `adapter/` immediately —
   before writing any real class.
4. Add the service's `Dockerfile` (multi-stage, non-root) and register it in
   `platform/compose/docker-compose.yml`.
5. Add an OpenAPI stub under `contracts/<service>/`.

### 3. Wire the aggregator reactor

```xml
<modules>
  <module>build-conventions</module>
  <module>services/gateway</module>
  <module>services/merchant-bff</module>
  <module>services/ops-bff</module>
  <module>services/merchant-service</module>
  <module>services/payment-orchestrator</module>
  <module>services/risk-service</module>
  <module>services/rail-adapter-service</module>
  <module>services/reconciliation-service</module>
  <module>services/reporting-service</module>
  <module>services/notification-service</module>
</modules>
```

Local convenience only — each service keeps its own version and Docker tag.

### 4. First commands to run once DS-002 lands

```bash
./mvnw test
./mvnw -pl services/payment-orchestrator test
./mvnw -pl services/payment-orchestrator spring-boot:run
docker compose -f platform/compose/docker-compose.yml up -d postgres-orchestrator
```

---

## Maven commands (once services exist)

```bash
./mvnw test
./mvnw -pl services/<name> test
./mvnw -pl services/<name> verify -Pintegration
./mvnw -pl services/<name> spring-boot:run
./mvnw verify
```

### Test tagging convention

- `@Tag("unit")` — fast, no external deps
- `@Tag("integration")` — Testcontainers
- `@Tag("contract")` — REST/AsyncAPI contract tests, including the `LedgerPort` contract
  against FinLedger's documented behavior (`rails/*`, `/splits`, `/refunds`)
- `@Tag("architecture")` — ArchUnit
- `@Tag("e2e")` — full EcoPay journey (payment + refund), release pipeline / on-demand only
- `@Tag("chaos")` — codified fault-injection experiments, on-demand / scheduled only

### Contract snapshot convention

Each service commits its own path inventory under
`contracts/<service>/openapi-paths.json`, checked by a contract test that fails the build
on undocumented drift — same discipline as FinLedger's own `docs/contracts/openapi-paths.json`
(FL-160).