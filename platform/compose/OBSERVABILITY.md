# Observability stack (DS-012)

Local OpenTelemetry collector UI via **Jaeger all-in-one** (OTLP). Not a PayHub
service database.

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

Spring Boot Micrometer OTLP (HTTP) expects the traces path:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
```

In Compose network (service DNS):

```bash
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces
```

Shared Config Server defaults live in `platform/config/application.yml`
(`management.tracing.sampling.probability`, `management.otlp.tracing.endpoint`).

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
