# build-conventions

Technical conventions shared across PayHub services. **No shared domain types** and
**no shared FinLedger SDK** (plan §2.3).

## ArchUnit checklist (per service)

Each service owns a FinLedger-shaped suite under
`src/test/java/com/payhub/<svc>/architecture/`:

| Class | Must enforce |
|-------|----------------|
| `ArchitectureTest` | `@Tag("architecture")`, `@AnalyzeClasses`, `ArchTests.in(...)` aggregator |
| `DomainRules` | Domain free of Spring, JPA/Hibernate, Kafka, Temporal, Redis, Micrometer/OTel; domain ↛ other layers |
| `ApplicationRules` | Application ↛ adapter, application ↛ infrastructure |
| `AdapterRules` | Adapter ↛ infrastructure, adapter ↛ domain |
| `InfrastructureRules` | Domain ↛ infrastructure; infrastructure ↛ adapter |
| `LayeredArchitectureRules` | Domain / Application / Adapter / Infrastructure with optional empty layers |

Dependency: `com.tngtech.archunit:archunit-junit5:1.4.2` (test scope).

When changing a rule in one service, update the other nine the same way.

## Allowed shared technical concerns (later tickets)

- Event envelope / AsyncAPI schemas under `contracts/`
- W3C `traceparent` helpers in `libraries/payhub-messaging` (`TraceParents`) — not domain
- Test fixtures that are not domain models

## Docker images (monorepo)

Build from the **repository root** so the Maven reactor can resolve `libraries/*`
and sibling module POMs:

```bash
docker build -f services/<name>/Dockerfile -t pauluno/payhub-<name>:local .
```

Release images (DS-018) go to GHCR: `ghcr.io/pauluno777/payhub-<name>:<semver>`
([ADR-008](../docs/adr/DS-ADR-008-ghcr-and-kind.md)).

See [`docs/runbooks/ghcr-release.md`](../docs/runbooks/ghcr-release.md) for tagging and the
DS-018 exit pull check. OTLP env vars for Compose / kind: `platform/compose/OBSERVABILITY.md`.
