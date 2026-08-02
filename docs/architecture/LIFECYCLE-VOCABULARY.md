# Lifecycle Vocabulary

## Purpose

Codex distinguishes normal CRUD-ish operations, lifecycle operations, retention/archive administration, and security administration.

This vocabulary keeps domain language explicit. It should help future permissions, service methods, audit events, retention policies, and adapter boundaries use the same words without overclaiming implementation that does not exist yet.

## Vocabulary

### CRUD-ish

- `read` = inspect current state.
- `create` = introduce a new active entity or draft entity.
- `update` = modify editable data while preserving identity.

### Lifecycle

- `start` = move a site into active operation.
- `suspend` = temporarily disable active operation.
- `publish` = make content available as live/published.
- `unpublish` = remove content from live availability without deleting it.
- `archive` = remove an entity from normal active use while preserving history and traceability.

### Retention / Archive Administration

- `restore` = future operation to bring archived material back from an archive/retention store.
- `purge` = future irreversible or policy-driven removal from archive/retention storage.

### Security Administration

- `grant` / `revoke` permissions.
- `assign` / `revoke` roles.

## Current Decision

ContentType authorization should use archive vocabulary, not delete vocabulary.

The current ContentType domain lifecycle uses `ARCHIVED` / `archive`, so permission names and authorization checks should align with that lifecycle language. Using delete vocabulary for ContentType authorization would imply a destructive or removal-oriented semantic that the current domain model does not express.

Current Custos vocabulary should therefore use:

- `CONTENT_TYPE_ARCHIVE`
- `contentType.archive`
- `canArchiveContentType`

and should not use `CONTENT_TYPE_DELETE`, `contentType.delete`, or `canDeleteContentType` for the current ContentType lifecycle.

## Open Design Questions

These questions are intentionally open:

- Is archive represented as a status in the active store, or as movement into an archive store?
- Should restore be supported?
- Should purge be policy-driven?
- Should `ArchiveStore` be a port under Archivum?
- Should there be adapters such as filesystem, object storage, PostgreSQL JSONB, or NoSQL?
- How do versions, languages, and variants affect restore/purge?

## Boundary Notes

- Do not claim Archivum archive storage exists yet.
- Do not claim restore or purge are implemented.
- Do not claim delete/destroy semantics are settled.
- `destroy` is currently avoided as terminology.
- `purge` is preferred for future irreversible cleanup.

## Reading Path

Related documents:

- `docs/CODEX-BLUEPRINT.md`
- `docs/security/CUSTOS-MODEL.md`
- `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md`
- `docs/future-forward/ADR-009.md`
