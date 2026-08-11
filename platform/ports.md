# PayHub ports

Logical service names are the registry keys (**Compose DNS = K8s CoreDNS**).  
Rename one side ⇒ rename the other and update this file. See [ADR-003](../docs/adr/DS-ADR-003-config-server-and-dns-registry.md).

In-network, every PayHub app and FinLedger listen on **8080** (API) and **8081** (management).

## Host publish (Compose / laptop)

| Logical name | Host API | Host management | Host Postgres |
|--------------|----------|-----------------|---------------|
| `finledger` | 8080 | 8081 | 5432 (`postgres-finledger`) |
| `config-server` | 8888 | — | — |
| `kafka` | 9092 | — | — |
| `schema-registry` | 8085 | — | — |
| `debezium-connect` | 8083 | — | — |
| `gateway` | 8200 | 8201 | — |
| `merchant-bff` | 8210 | 8211 | — |
| `ops-bff` | 8220 | 8221 | — |
| `merchant-service` | 8300 | 8301 | 5433 (`postgres-merchant`) |
| `payment-orchestrator` | 8310 | 8311 | 5434 (`postgres-orchestrator`) |
| `risk-service` | 8320 | 8321 | 5435 (`postgres-risk`) |
| `rail-adapter-service` | 8330 | 8331 | 5436 (`postgres-rail`) |
| `reconciliation-service` | 8400 | 8401 | 5437 (`postgres-reconciliation`) |
| `reporting-service` | 8410 | 8411 | 5438 (`postgres-reporting`) |
| `notification-service` | 8420 | 8421 | 5439 (`postgres-notification`) |
| `redis` (Reporting cache-aside, DS-013) | 6379 | — | — |
| `temporal` | 7233 (gRPC) | — | 5440 (`postgres-temporal`) |
| `zitadel` (profile `identity`) | 8090 | — | see IdP rows |
| `postgres-zitadel` (lab default IdP DB — ADR-007 amended) | — | — | TBD host map on Compose switch |
| `cockroachdb` (optional profile `identity-crdb`, Zitadel only) | 26257 (SQL) | 8086 (UI) | — |
| `jaeger` (profile `observability`) | 16686 (UI) | 4317 OTLP gRPC / 4318 OTLP HTTP | — |
| `rabbitmq` (profile `rabbitmq`, lab only) | 5672 (AMQP) | 15672 (management UI) | — |

## Kind CoreDNS (namespace `payhub`)

Apps use the **same short names** as Compose (`http://risk-service:8080`,  
`jdbc:postgresql://postgres-orchestrator:5432/orchestrator`).  
In-cluster FQDN (rarely needed): `<name>.payhub.svc.cluster.local`.

| Logical name (`Service` / Compose key) | In-cluster API | Notes |
|----------------------------------------|----------------|-------|
| `config-server` | `8888` | Optional in DS-019; DS-020 may re-enable |
| `gateway` | `8080` / `8081` | |
| `merchant-bff` | `8080` / `8081` | |
| `ops-bff` | `8080` / `8081` | |
| `merchant-service` | `8080` / `8081` | |
| `payment-orchestrator` | `8080` / `8081` | DS-019 lab (×2 replicas) |
| `risk-service` | `8080` / `8081` | |
| `rail-adapter-service` | `8080` / `8081` | |
| `reconciliation-service` | `8080` / `8081` | |
| `reporting-service` | `8080` / `8081` | |
| `notification-service` | `8080` / `8081` | |
| `finledger` | `8080` / `8081` | pinned Docker Hub image |
| `kafka` | `9092` | KRaft 1 broker |
| `schema-registry` | `8081` (in-network) | host publish 8085 |
| `debezium-connect` | `8083` | |
| `redis` | `6379` | |
| `temporal` | `7233` | DS-019 lab |
| `zitadel` | `8080` or vendor port | OIDC issuer |
| `postgres-finledger` | `5432` | |
| `postgres-merchant` | `5432` | |
| `postgres-orchestrator` | `5432` | DS-019 lab |
| `postgres-risk` | `5432` | |
| `postgres-rail` | `5432` | **not** `postgres-railadapter` |
| `postgres-reconciliation` | `5432` | |
| `postgres-reporting` | `5432` | |
| `postgres-notification` | `5432` | |
| `postgres-temporal` | `5432` | DS-019 lab |
| `postgres-zitadel` | `5432` | IdP lab default (ADR-007) |

**Images:** PayHub apps = `ghcr.io/pauluno777/payhub-<service>:<semver>` or `:local`  
(same Dockerfiles as Compose). Infra pins match [`compose/docker-compose.yml`](compose/docker-compose.yml)  
(`postgres:17-alpine`, Temporal, Kafka, FinLedger).

**Config:** Compose profile YAML hostnames are the source of truth  
([`config/*-compose.yml`](config/)). Kind ConfigMaps must project the **same** hostnames;  
they are not a second naming scheme.

Config: Spring Cloud Config Server (`config-server:8888`) serves [`platform/config/`](config/).  
Profiles: `local` (IDE host ports), `compose` (in-network DNS).

OIDC (DS-011): `docker compose --profile identity up -d` — see [`IDENTITY.md`](compose/IDENTITY.md).  
Issuer (host): `http://localhost:8090`. Lab default IdP datastore = `postgres-zitadel`  
(ADR-007 amended); Cockroach remains optional profile `identity-crdb` (Compose switch = follow-up).

Observability (DS-012): `docker compose --profile observability up -d` — see [`OBSERVABILITY.md`](compose/OBSERVABILITY.md).
OTLP HTTP (host): `http://localhost:4318/v1/traces`. Jaeger UI: `http://localhost:16686`.

RabbitMQ lab (DS-014): `docker compose --profile rabbitmq up -d` — see [`RABBITMQ_LAB.md`](compose/RABBITMQ_LAB.md).
Management UI: `http://localhost:15672` (`payhub` / `payhub`). Not required for webhook DLQ exit path.  
Not part of the kind always-on footprint.

Kafka clients (Compose/kind): `kafka:9092`. Schema Registry (Compose): `http://schema-registry:8081`.  
Host Schema Registry: `http://localhost:8085`. CDC connector API: `http://localhost:8083`.  
Temporal (Compose/kind short name): `temporal:7233` / host `localhost:7233`.  
Redis (Compose/kind): `redis:6379` / host `localhost:6379`.
