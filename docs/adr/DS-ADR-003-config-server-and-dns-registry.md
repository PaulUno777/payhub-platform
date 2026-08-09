# ADR-003 — Config Server and DNS service registry

- **Status:** Accepted
- **Date:** 2026-08-09
- **Ticket:** DS-003

## Context

PayHub runs ten (plus growing) Spring Boot services. Hardcoded `localhost` ports and
per-service `.properties` do not scale, collide with FinLedger’s 8080/8081/5432, and
fight Compose/Kubernetes discovery.

## Decision

1. **Service registry:** Compose DNS today, Kubernetes CoreDNS later (plan §2.0). No
   Eureka/Consul/Nacos. Clients call `http://<logical-name>:8080`.
2. **Configuration:** Spring Cloud Config Server (`services/config-server`) with
   **native** filesystem backend over git-versioned [`platform/config/`](../../platform/config/)
   YAML and multi-profiles (`local`, `compose`). Secrets stay in environment variables.
3. **Ports:** In-network 8080/8081 for every app; host publish table in
   [`platform/ports.md`](../../platform/ports.md). FinLedger reserved at 8080/8081/5432.

## Consequences

- Config Server must start before (or be optional for) clients; tests use
  `optional:configserver:` plus classpath test YAML.
- Future DS-018 can swap native → git URI / ConfigMaps without changing client import shape.
- Rejects a second discovery system alongside Kubernetes DNS.
