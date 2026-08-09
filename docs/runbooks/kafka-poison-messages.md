# Kafka poison messages (DS-016)

PayHub consumers (Reporting, Notification) use finite in-listener retries, then isolate
failures on `{originalTopic}.dlq`. Merchant webhook `DEAD` rows (DS-014) are **not** this
path — those are HTTP delivery exhaustion in Postgres.

## Detect

- Consumer lag on `payment.lifecycle.v1` / `ledger.journal-entry.v1` while the service is healthy.
- Log lines from Spring Kafka `DefaultErrorHandler` / `DeadLetterPublishingRecoverer`.
- Depth on `{topic}.dlq` (consume or inspect with a short-lived consumer group).

## Isolate (automatic)

1. Retryable failures: up to 3 attempts with 500 ms backoff.
2. Non-retryable (parse / deser / `IllegalArgumentException` / Jackson): immediate publish to
   `{topic}.dlq`, original offset committed — other partitions keep processing
   (`payhub.kafka.concurrency` defaults to 2).
3. After retries are exhausted for a retryable error: same DLQ publish.

## Inspect

Read the DLQ payload and headers (business headers such as `eventId` / `traceparent` are
preserved; Spring `kafka_dlt-*` headers describe the failure).

## Replay

After fixing the root cause (schema, mapping, or transient dependency):

```bash
curl -sS -X POST "http://localhost:<reporting-port>/api/v1/reporting/events/dlq/replay" \
  -H "Content-Type: application/json" \
  -d '{"dlqTopic":"ledger.journal-entry.v1.dlq","maxRecords":50}'
```

Replay uses consumer group `reporting-dlq-replay` (committed offsets) and republishes to the
topic derived by stripping `.dlq`. Inbox-before-action still deduplicates by `eventId`.

## Quotas and rebalances (notes only)

- Prefer raising application concurrency / partition count before broker producer/consumer
  quotas; quotas are a cluster ops concern, not a PayHub API in v1.
- Skipping a poison offset can trigger a normal group rebalance if the consumer session
  times out during a long failure path; with non-retryable classification that window is
  short. No custom rebalance listener is required for v1.

## Out of scope (deferred)

Dedicated `{topic}.retry.*` delayed-retry chains. v1 keeps in-listener `FixedBackOff` +
`.dlq` isolation (plan §6.2).
