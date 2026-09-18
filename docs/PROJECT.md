# Codex Project

Codex is a Java 25 modular-monolith CMS for managing structured knowledge through sites,
content types, content items, revisions, lifecycle operations, projections, and domain
authorization. Its kernel is intentionally transport- and persistence-agnostic.

## Current State

The working system is an in-memory CMS kernel with lifecycle services, deferred domain events,
cache invalidation, public-content indexing, Chronicon domain audit, Observance metrics, and a
secured Custos runtime path. Persistence, transport/API exposure, workflow, direct actor grants,
collection-read filtering, and authorization-decision records remain deferred. The
[CODEX-OSK-001 reality check](engineering/agents/reviews/transversal/CODEX-OSK-001—CurrentStateRealityCheck—REVIEW.md)
is the repository-grounded status evidence for this summary.

## Where To Start

1. Read [OSK.md](OSK.md) to classify the artifact you need to create or change.
2. Read the canonical [roadmap](engineering/roadmap/ROADMAP.md) for committed and deferred direction.
3. Read relevant [architecture](knowledge/architecture/README.md), [domain](knowledge/domain/README.md), or
   [security](knowledge/security/README.md) knowledge.
4. Read applicable [ADRs](engineering/adr/README.md) for durable decisions and
   [engineering evidence](engineering/ENGINEERING_LOG.md) for delivery history.
5. For active work, use [agent tasks](engineering/agents/tasks/README.md), then record outcomes
   under [reports](engineering/agents/reports/README.md) or [reviews](engineering/agents/reviews/README.md).

## Repository Map

| Area | Canonical location |
| --- | --- |
| Committed direction | [roadmap/ROADMAP.md](engineering/roadmap/ROADMAP.md) |
| Non-committed proposals | [roadmap/future/](engineering/roadmap/future/) |
| Architecture and conceptual material | [architecture/](knowledge/architecture/) |
| Domain concepts and historical MVP model | [domain/](knowledge/domain/) |
| Authorization | [security/](security/) |
| Architecture decisions | [adr/](engineering/adr/) |
| Durable current knowledge | [knowledge/](knowledge/) |
| Engineering tasks, reports, reviews, checkpoints | [engineering/](engineering/) |
| Research evidence | [research/](research/) |
| Module boundaries | [modules/](knowledge/modules/) |

## Technology And Validation

Codex uses Maven, JPMS, and Java 25. The normal full verification command is:

```bash
mvn clean verify
```

Documentation-only work should validate links and run `git diff --check`; it does not require
tests unless the task says otherwise.
