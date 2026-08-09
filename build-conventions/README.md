# build-conventions

Technical conventions shared across PayHub services. **No shared domain types** and
**no shared FinLedger SDK** (plan §2.3).

## ArchUnit checklist (per service)

Each service owns a FinLedger-shaped suite under
`src/test/java/com/payhub/<svc>/architecture/`:

| Class | Must enforce |
|-------|----------------|
| `ArchitectureTest` | `@Tag("architecture")`, `@AnalyzeClasses`, `ArchTests.in(...)` aggregator |
| `DomainRules` | Domain free of Spring, JPA/Hibernate, Kafka, Temporal, Redis; domain ↛ other layers |
| `ApplicationRules` | Application ↛ adapter, application ↛ infrastructure |
| `AdapterRules` | Adapter ↛ infrastructure, adapter ↛ domain |
| `InfrastructureRules` | Domain ↛ infrastructure; infrastructure ↛ adapter |
| `LayeredArchitectureRules` | Domain / Application / Adapter / Infrastructure with optional empty layers |

Dependency: `com.tngtech.archunit:archunit-junit5:1.4.2` (test scope).

When changing a rule in one service, update the other nine the same way.

## Allowed shared technical concerns (later tickets)

- Event envelope / AsyncAPI schemas under `contracts/`
- W3C `traceparent` helpers
- Test fixtures that are not domain models
