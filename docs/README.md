# PayHub documentation

Start here, then go deep.

| Doc | When to read |
|-----|----------------|
| [PLAN_PAYHUB.md](PLAN_PAYHUB.md) | Full product/architecture plan (source of truth) |
| [architecture.md](architecture.md) | Hexagonal shape + FinLedger ownership |
| [development.md](development.md) | DS-001…024 tickets, bootstrap commands |
| [context-map.md](context-map.md) | Bounded contexts & relationships (DS-001) |
| [event-catalog.md](event-catalog.md) | Kafka/RabbitMQ topics (stub → AsyncAPI later) |
| [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md) | Undecided assumptions — update before guessing in code |
| [adr/](adr/) | Structural decisions |
| [runbooks/](runbooks/) | Ops procedures (filled as tickets land) |

**PayHub does not:** store authoritative balances, recompute FinLedger splits/fee
reversals, fork FinLedger, or point MmSandbox webhooks at FinLedger’s HMAC endpoint.

Topology sketch: plan §2.0 · ownership: architecture.md · FinLedger pin: `../finledger/`.
