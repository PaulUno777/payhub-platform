# platform/config

Git-versioned Spring Cloud Config source (native filesystem backend).

Naming: `application.yml`, `application-{{profile}}.yml`, `{{spring.application.name}}.yml`,
`{{spring.application.name}}-{{profile}}.yml`.

Profiles: `local` (IDE), `compose` (Docker Compose network).

Secrets never live here — use environment variables.
