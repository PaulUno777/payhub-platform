# Architecture Decision Records

Naming: `DS-ADR-NNN-short-slug.md` (PayHub) — distinct from FinLedger’s `ADR-NNN`.

| ADR | Title | Status |
|-----|-------|--------|
| [DS-ADR-001](DS-ADR-001-cap-pacelc.md) | CAP / PACELC choices per component | Accepted (DS-001) |
| [DS-ADR-002](DS-ADR-002-hexagonal-ten-services.md) | Hexagonal per service + 10-service start | Accepted (DS-002) |
| [DS-ADR-003](DS-ADR-003-config-server-and-dns-registry.md) | Config Server + DNS registry (Compose = kind CoreDNS) | Amended (DS-003; 2026-08-10) |
| [DS-ADR-004](DS-ADR-004-finledger-outbox-cdc.md) | FinLedger outbox → Debezium → Kafka | Accepted (DS-005) |
| [DS-ADR-005](DS-ADR-005-temporal-orchestration.md) | Temporal for payment/refund orchestration | Accepted (DS-006) |
| [DS-ADR-006](DS-ADR-006-payhub-messaging-outbox-inbox.md) | PayHub messaging inbox/outbox helpers | Accepted (DS-007) |
| [DS-ADR-007](DS-ADR-007-zitadel-oidc-cockroach.md) | Zitadel OIDC + IdP datastore (Postgres lab default; Cockroach optional) | Amended (DS-011; 2026-08-10) |
| [DS-ADR-008](DS-ADR-008-ghcr-and-kind.md) | PayHub images on GHCR; kind single-node default | Amended (DS-018; 2026-08-10) |

Rules: one ADR per irreversible structural choice. Update `PLAN_PAYHUB.md` §19 when
accepted. Never resolve an `OPEN_QUESTIONS.md` item in code without an ADR or plan edit.
