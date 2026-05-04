# codex-archivum

> The Archivum stores the manuscripts.

## Responsibility

`codex-archivum` owns the durable storage layer of the Codex CMS. It will eventually hold:

- Durable repository implementations (PostgreSQL, filesystem, object storage)
- Relational database adapters
- S3/MinIO-style binary asset storage
- Transaction adapters for durable backends
- Migration support (Flyway or Liquibase)
- Outbox storage (future)

## Current Status

Skeleton only. No persistence backend is implemented yet.

In-memory repository implementations currently live inside `codex-codex` during MVP. A future
task will introduce durable adapters here and wire them through the runtime.

## Dependency Rules

- Implements persistence; does **not** own domain lifecycle.
- Does not own search, cache, audit, workflow, rendering, or AI enrichment.
- Depends inward on core repository interfaces and entity contracts.
- Repository implementations must not contain business logic.
- No JDBC, JPA, Flyway, Liquibase, HikariCP, PostgreSQL, S3, or MinIO dependencies are added
  in this skeleton.
