# Codex

Codex is a Java 25 modular-monolith CMS for structured knowledge. Its domain kernel manages
sites, content types, content items, revisions, lifecycle transitions, projections, and
transport-agnostic domain authorization.

The project is deliberately domain-first: HTTP exposure, persistence providers, workflow, and AI
integration are separate concerns rather than dependencies of the core model.

## Current State

The implemented system is an in-memory CMS kernel with:

- Site, ContentType, ContentItem, and ContentRevision lifecycle services.
- Deferred domain-event dispatch, cache invalidation, public-content indexing, and Chronicon
  domain audit.
- Observance counters and timers for core runtime paths.
- Custos permission resolution, domain permission services, secured service decorators, and a
  `ConciliumRuntime.secured(...)` composition path.

Persistence, transport/API exposure, workflow, collection-read filtering, direct actor grants,
structured authorization traces, and authorization decision records remain deferred. The
[CODEX-OSK-001 reality check](docs/engineering/agents/reviews/transversal/CODEX-OSK-001—CurrentStateRealityCheck—REVIEW.md)
contains the repository-grounded capability inventory and known gaps.

## Build And Test

Codex uses Maven, JPMS, and Java 25.

```bash
mvn clean verify
```

## Documentation

Start with [project context](docs/PROJECT.md), then follow the OSK documentation model.

| Need | Canonical document or area |
| --- | --- |
| Project orientation and navigation | [docs/PROJECT.md](docs/PROJECT.md) |
| Documentation placement rules | [docs/OSK.md](docs/OSK.md) |
| Durable current knowledge | [docs/knowledge/](docs/knowledge/README.md) |
| Architecture and module boundaries | [Blueprint](docs/knowledge/architecture/CODEX-BLUEPRINT.md) and [module responsibilities](docs/knowledge/modules/MODULE-RESPONSIBILITIES.md) |
| Authorization model and status | [Custos model](docs/knowledge/security/CUSTOS-MODEL.md) and [checklist](docs/knowledge/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md) |
| Committed direction | [roadmap](docs/engineering/roadmap/ROADMAP.md) |
| Architectural rationale | [ADRs](docs/engineering/adr/README.md) |
| Engineering history | [engineering log](docs/engineering/ENGINEERING_LOG.md) |
| Tasks, reports, reviews, and checkpoints | [engineering agent artifacts](docs/engineering/agents/README.md) |

## Module Boundaries

`codex-fundamentum` provides shared primitives. `codex-codex` owns the CMS domain kernel.
`codex-index` and `codex-chronicon` consume projections; `codex-custos` owns domain
authorization; `codex-concilium` composes the local runtime. Edge modules remain intentionally
separate and mostly deferred.

The detailed boundary map is maintained in
[docs/knowledge/modules/MODULE-RESPONSIBILITIES.md](docs/knowledge/modules/MODULE-RESPONSIBILITIES.md).
