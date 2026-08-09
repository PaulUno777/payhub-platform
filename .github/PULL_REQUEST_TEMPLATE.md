## Summary

Briefly describe what this PR does and why.

## Ticket

`DS-0xx` / roadmap phase:

## Checklist

- [ ] Tests added or updated for the behavior change
- [ ] ArchUnit still green (`domain` remains framework-free; rules under each service `…/architecture/`)
- [ ] No secrets committed; CORS / tenant isolation not weakened for convenience
- [ ] `docs/PLAN_PAYHUB.md` and/or ADR updated if this is structural
- [ ] Conventional Commits; one concern per commit
- [ ] Out of scope for this ticket left for later DS-0xx work

## Test plan

- [ ] `./mvnw -B test` from the reactor root
- [ ] Scoped runs when useful: `./mvnw -pl services/<name> test`
- [ ] Integration / Testcontainers (if this ticket touches persistence or messaging)
- [ ] Compose (if infra touched): `docker compose -f platform/compose/docker-compose.yml up -d`
