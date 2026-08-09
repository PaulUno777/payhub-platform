# PayHub ports

Logical service names are the registry keys (Compose DNS → K8s CoreDNS).  
In-network, every PayHub app and FinLedger listen on **8080** (API) and **8081** (management).

| Logical name | Host API | Host management | Host Postgres |
|--------------|----------|-----------------|---------------|
| `finledger` | 8080 | 8081 | 5432 |
| `config-server` | 8888 | — | — |
| `kafka` | 9092 | — | — |
| `schema-registry` | 8085 | — | — |
| `debezium-connect` | 8083 | — | — |
| `gateway` | 8200 | 8201 | — |
| `merchant-bff` | 8210 | 8211 | — |
| `ops-bff` | 8220 | 8221 | — |
| `merchant-service` | 8300 | 8301 | 5433 |
| `payment-orchestrator` | 8310 | 8311 | 5434 |
| `risk-service` | 8320 | 8321 | 5435 |
| `rail-adapter-service` | 8330 | 8331 | 5436 |
| `reconciliation-service` | 8400 | 8401 | 5437 |
| `reporting-service` | 8410 | 8411 | 5438 |
| `notification-service` | 8420 | 8421 | 5439 |
| `temporal` | 7233 (gRPC) | — | 5440 (`postgres-temporal`) |
| `zitadel` (profile `identity`) | 8090 | — | — (state in Cockroach) |
| `cockroachdb` (profile `identity`, Zitadel only) | 26257 (SQL) | 8086 (UI) | — |
| `jaeger` (profile `observability`) | 16686 (UI) | 4317 OTLP gRPC / 4318 OTLP HTTP | — |

Config: Spring Cloud Config Server (`config-server:8888`) serves [`platform/config/`](config/).  
Profiles: `local` (IDE host ports), `compose` (in-network DNS).

OIDC (DS-011): `docker compose --profile identity up -d` — see [`IDENTITY.md`](compose/IDENTITY.md).
Issuer (host): `http://localhost:8090`.

Observability (DS-012): `docker compose --profile observability up -d` — see [`OBSERVABILITY.md`](compose/OBSERVABILITY.md).
OTLP HTTP (host): `http://localhost:4318/v1/traces`. Jaeger UI: `http://localhost:16686`.

Kafka clients (Compose): `kafka:9092`. Schema Registry (Compose): `http://schema-registry:8081`.  
Host Schema Registry: `http://localhost:8085`. CDC connector API: `http://localhost:8083`.  
Temporal (Compose/host): `temporal:7233` / `localhost:7233`.
