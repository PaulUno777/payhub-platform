# ADR-002 — Ten hexagonal Spring Boot services from day one

- **Status:** Accepted
- **Date:** 2026-08-08
- **Ticket:** DS-002

## Context

PayHub is a learning platform for distributed-systems problems across real service
boundaries. A single modular monolith would hide network/idempotency/saga failure modes
that the roadmap (§17) intentionally teaches.

## Decision

Scaffold **ten independently releasable Spring Boot 4.1.0 services** under `services/`,
each with hexagonal packages (`domain`, `application`, `infrastructure`, `adapter`) and a
FinLedger-shaped ArchUnit suite. Shared code is limited to `build-conventions` (checklist /
build hygiene) — **no shared domain**, **no shared FinLedger SDK** (plan §2.3).

## Consequences

- Higher bootstrap cost and CI surface at DS-002.
- Progressive dependency addition (Kafka, Temporal, Redis, AMQP) stays per-ticket.
- Cross-service consistency must use APIs, events, and sagas — never shared DB schemas.
