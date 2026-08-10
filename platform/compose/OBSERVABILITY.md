# Observability stack (DS-012)

Local OpenTelemetry collector UI via **Jaeger all-in-one** (OTLP). Not a PayHub
service database.

Architecture note: tracing stays in **infrastructure / adapter / messaging** —
never in `domain` (ArchUnit) and never a shared domain module. `TraceParents` in
`libraries/payhub-messaging` is a technical envelope helper only.

## Start

```bash
cd platform/compose
docker compose --profile observability up -d jaeger
```

| URL / port | Purpose |
|------------|---------|
| http://localhost:16686 | Jaeger UI |
| `localhost:4317` | OTLP gRPC |
| `localhost:4318` | OTLP HTTP |

## Point PayHub services at Jaeger

Spring Boot Micrometer OTLP (HTTP) expects the traces path.

**Host / IDE** (`local` profile):

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
export TRACING_OTLP_ENABLED=true
```

**Compose network** (`compose` profile — see `application-compose.yml`):

```bash
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces
TRACING_OTLP_ENABLED=true
```

OTLP export defaults to **off** in Compose so services do not error-loop when the
`observability` profile is down. Turn it on when Jaeger is up.

Shared Config Server defaults also live in `platform/config/application.yml`
(`management.tracing.sampling.probability`).

## Docker images (learning path toward DS-018 / DS-019)

Build **from the repository root** (monorepo reactor needs `libraries/` + all module
POMs):

```bash
docker build -f services/payment-orchestrator/Dockerfile -t pauluno/payhub-payment-orchestrator:local .
docker build -f services/reporting-service/Dockerfile -t pauluno/payhub-reporting-service:local .
```

Release images (DS-018) publish to GHCR as `ghcr.io/pauluno777/payhub-<service>:<semver>`
via [`.github/workflows/release.yml`](../../.github/workflows/release.yml) — see
[ADR-008](../../docs/adr/DS-ADR-008-ghcr-and-kind.md) and the cut/verify runbook
[`docs/runbooks/ghcr-release.md`](../../docs/runbooks/ghcr-release.md).
Local `:local` tags remain the Compose / `kind load` fast path.

In-container ports match Compose/K8s: API `8080`, management `8081` (health on
`8081/actuator/health`). Host port mapping stays in `platform/ports.md`.

For local Kubernetes later (DS-019, **kind**): same images; set the two env vars above to the
in-cluster Jaeger/OTel collector Service DNS (e.g. `http://jaeger:4318/v1/traces`).
Do not bake endpoints into the image.

## FinLedger (optional, same collector)

FinLedger accepts `OTEL_EXPORTER_OTLP_ENDPOINT` (see `finledger/INTEGRATION_GUIDE.md`).
To join the same Jaeger when the FinLedger container is running:

```yaml
# example override — do not fork FinLedger
environment:
  OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318
```

(FinLedger’s SDK may use the base OTLP URL without `/v1/traces`; PayHub Boot uses the
full traces URL above.)

## Propagation

- HTTP: W3C `traceparent` (Micrometer Tracing + RestClient observation)
- Kafka: `EventEnvelope.traceparent` on `payment.lifecycle.v1` (Reporting continues the span)

Temporal workflow/activity span linking is out of scope for DS-012.
Kubernetes Service / Deployment wiring is DS-019 — see [`platform/k8s/`](../../platform/k8s/)
and [`docs/runbooks/kind-temporal-failover.md`](../../docs/runbooks/kind-temporal-failover.md).
Image publish to GHCR is DS-018 (precondition for prod-like pulls).
