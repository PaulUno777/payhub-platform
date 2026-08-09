# Dev Container (DS-011)

Uses Java 21 + Docker-in-Docker. On create, starts the Compose **`identity`** profile
(Zitadel + CockroachDB). See [`platform/compose/IDENTITY.md`](../platform/compose/IDENTITY.md).

PayHub service Postgres / Kafka / Temporal are **not** started by default — bring them
up with the main compose file when needed.
