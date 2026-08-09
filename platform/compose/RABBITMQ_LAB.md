# RabbitMQ lab (DS-014)

Optional local broker for comparing **work-queue fan-out** against PayHub's
production notification path (Kafka lifecycle → Postgres `WebhookDelivery` →
HMAC HTTP retries → status `DEAD`).

RabbitMQ is **not** the v1 event backbone and is **not** required for the
DS-014 exit criterion (failed webhook ends in an observable DLQ).

## Start

```bash
cd platform/compose
docker compose --profile rabbitmq up -d rabbitmq
```

| URL / port | Purpose |
|------------|---------|
| `localhost:5672` | AMQP |
| http://localhost:15672 | Management UI (`payhub` / `payhub`) |

## When a work queue might help

| Concern | Kafka + Postgres DEAD (exit path) | Rabbit work queue |
| --- | --- | --- |
| Merchant HTTP retries | Rows + `@Scheduled` poll, exponential backoff on the row | Per-message TTL / dead-letter exchange |
| Fan-out to many workers | Single consumer group + DB claim | Competing consumers on a queue |
| Observability of poison | `GET /ops/notifications/dlq` (status `DEAD`) | DLX + management UI |
| Dual backbone cost | One broker | Kafka **and** Rabbit ops surface |

Use the lab to measure operator UX (management UI vs Ops DLQ API), not to
replace `payment.lifecycle.v1`. If Rabbit ever becomes more than a lab, open an
ADR first (plan §6.3 / §19).

## Exit path remains

1. Consume `payment.lifecycle.v1` (inbox).
2. Persist `WebhookDelivery`.
3. HMAC POST with in-process retries.
4. After max attempts → `DEAD`, listed on Ops DLQ — never silently dropped.
