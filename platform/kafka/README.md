# PayHub Kafka / CDC (DS-005)

## Components (Compose)

| Service | Image (pinned) | Role |
|---------|----------------|------|
| `kafka` | `apache/kafka:3.8.1` | KRaft broker |
| `schema-registry` | `confluentinc/cp-schema-registry:7.6.1` | JSON Schema registry (KISS vs Avro) |
| `debezium-connect` | `debezium/connect:2.7.3.Final` | CDC from FinLedger `outbox_event` |

Topic: **`ledger.journal-entry.v1`** (plan §6.1).

## Register the outbox connector

After FinLedger Postgres is healthy and at least one migration created `outbox_event`:

```bash
./platform/kafka/register-connector.sh
```

Connector JSON: [`connectors/finledger-outbox.json`](connectors/finledger-outbox.json).

## Column mapping (FinLedger → Kafka)

See [ADR-004](../../docs/adr/DS-ADR-004-finledger-outbox-cdc.md). Payload field names
match FinLedger `TransactionPosted` — never invent columns.
