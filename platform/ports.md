# PayHub ports

Logical service names are the registry keys (Compose DNS → K8s CoreDNS).  
In-network, every PayHub app and FinLedger listen on **8080** (API) and **8081** (management).

| Logical name | Host API | Host management | Host Postgres |
|--------------|----------|-----------------|---------------|
| `finledger` | 8080 | 8081 | 5432 |
| `config-server` | 8888 | — | — |
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

Config: Spring Cloud Config Server (`config-server:8888`) serves [`platform/config/`](config/).  
Profiles: `local` (IDE host ports), `compose` (in-network DNS).
