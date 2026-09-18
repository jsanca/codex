# Codex Roadmap

This is the one canonical record of committed project direction. Completed work is linked to
engineering evidence; non-committed designs belong in [future/](future/).

## Current

| Item | Status | Evidence |
| --- | --- | --- |
| CODEX-KNOW-006: normalize documentation to OSK structure | In progress | Task direction supplied to the current documentation-curation work |

## Recently Completed

| Capability | Status | Evidence |
| --- | --- | --- |
| CMS lifecycle kernel, event pipeline, cache invalidation, indexing, Chronicon audit, and Observance | Implemented and verified | [CODEX-OSK-001](../agents/reviews/transversal/CODEX-OSK-001—CurrentStateRealityCheck—REVIEW.md) |
| Custos resolver, decision service, domain permission services, secured decorators, and secured runtime composition | Implemented and verified | [CODEX-OSK-001](../agents/reviews/transversal/CODEX-OSK-001—CurrentStateRealityCheck—REVIEW.md) |

## Deferred

- Permission vocabulary and gated secured operations for item delete/restore and site unarchive.
- Collection-read filtering and alias-to-`SiteKey` authorization.
- Direct actor grants, structured authorization traces, security-audit routing, authorization metrics,
  and permission-decision caching.
- Persistence, transport/API exposure, workflow, and the currently skeletal edge modules.

These are evidence-backed deferrals, not commitments. See the
[reality check](../agents/reviews/transversal/CODEX-OSK-001—CurrentStateRealityCheck—REVIEW.md)
for their exact status and scope.

## Historical Direction

The earlier working-phase roadmap is retained as historical context in
[history/CODEX-ROADMAP-PHASES.md](history/CODEX-ROADMAP-PHASES.md). It is not a second roadmap.
