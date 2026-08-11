# ADR-003 — Config Server and DNS service registry

- **Status:** Amended
- **Date:** 2026-08-09
- **Amended:** 2026-08-10
- **Ticket:** DS-003 (decision); amendment for Compose ↔ kind parity

## Context

PayHub runs ten (plus growing) Spring Boot services. Hardcoded `localhost` ports and
per-service `.properties` do not scale, collide with FinLedger’s 8080/8081/5432, and
fight Compose/Kubernetes discovery.

Kind (DS-019+) must not invent a second naming scheme: JDBC/HTTP hostnames in
[`platform/config/*-compose.yml`](../../platform/config/) must resolve identically under
Compose DNS and CoreDNS.

## Decision

1. **Service registry:** Compose DNS and Kubernetes CoreDNS use the **same logical
   names**. No Eureka/Consul/Nacos. Clients call `http://<logical-name>:8080` (and
   `jdbc:postgresql://postgres-*:5432/...` for DBs). Catalog:
   [`platform/ports.md`](../../platform/ports.md).
2. **Rename rule:** changing a Compose service key, a kind `Service` `metadata.name`,
   or a hostname in compose-profile config requires updating **all three** plus
   `ports.md` in the same change.
3. **Configuration:** Spring Cloud Config Server (`services/config-server`) with
   **native** filesystem backend over git-versioned [`platform/config/`](../../platform/config/)
   YAML and multi-profiles (`local`, `compose`). Secrets stay in environment variables.
   Kind may project the same property values via ConfigMaps (DS-019 lab) without
   changing hostnames; re-enabling in-cluster `config-server` (DS-020+) keeps the
   same client import shape.
4. **Ports:** In-network 8080/8081 for every app; host publish table in
   [`platform/ports.md`](../../platform/ports.md). FinLedger reserved at 8080/8081/5432
   on the Compose host map.
5. **Images:** PayHub apps use the same Dockerfiles / GHCR tags (or `:local`) as
   Compose; infra image pins match `platform/compose/docker-compose.yml`.

## Consequences

- Config Server must start before (or be optional for) clients; tests use
  `optional:configserver:` plus classpath test YAML.
- DS-019 Temporal lab already uses Compose-identical names
  (`payment-orchestrator`, `postgres-orchestrator`, `postgres-temporal`, `temporal`).
- DS-020 mesh must extend the same catalog (including `postgres-rail`, not
  `postgres-railadapter`).
- Rejects a second discovery system alongside Kubernetes DNS; rejects kind-only
  hostnames that diverge from Compose.
