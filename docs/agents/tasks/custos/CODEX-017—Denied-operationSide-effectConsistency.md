# Task CODEX-017 — Denied-operation side-effect consistency

Status: Planned
Owner: Clio
Role: Implementation
Target: 30–45 minutes
Hard Stop: 60 minutes

## Execution Requirements

Apply, if available:

Apply .claude/skills/process/osk-execution-timebox/SKILL.md
Apply .claude/skills/process/osk-engineering-reporting/SKILL.md

No commits.

Create an implementation report under:

* `docs/agents/reports/runtime/`

Create or update checkpoint if the task completes or reaches hard stop.

## Objective

Add service-level/runtime tests proving that denied operations in `ConciliumRuntime.secured(...)` do not produce side effects.

A denied operation must not:

* mutate domain state
* emit domain events
* invalidate or update cache state
* update index projections
* create Chronicon audit records accidentally

## Context

CODEX-016 completed secured runtime composition.

Current secured path:

* `ConciliumRuntime.secured(snapshotProvider)`
* exposes secured services via:

    * `runtime.siteService()`
    * `runtime.contentTypeService()`
    * `runtime.contentItemService()`

The raw core runtime remains available through `runtime.coreRuntime()`, but domain callers and adapters should use the top-level service accessors.

Existing tests already cover some denial behavior, but this task should expand and formalize side-effect consistency across the runtime pipeline.

## Scope

Tests first.

Add or extend tests in `codex-concilium`, likely:

* `ConciliumRuntimeSecuredTest`
* or a new focused test class:

    * `ConciliumRuntimeDeniedSideEffectsTest`

Production code changes are allowed only if a real bug is found.

## In Scope

Verify denied side effects for representative operations.

At minimum cover:

1. Denied site create

Actor without permissions attempts:

* `runtime.siteService().create(...)`

Assert:

* `AccessDeniedException` thrown
* site is not created
* no domain events recorded
* no Chronicon audit projection created if observable
* no index projection changed if observable

2. Denied content type create or archive

Given an existing authorized site if needed.

Actor without permissions attempts:

* `runtime.contentTypeService().create(...)`
* or `archive(...)`

Assert:

* denied
* content type state unchanged
* no domain events recorded
* no audit/index side effects

3. Denied content item publish

Given authorized setup creates site, content type, and draft item.

Then unauthorized actor attempts:

* `runtime.contentItemService().publish(...)`

Assert:

* denied
* item remains unpublished/draft
* no publish event emitted
* index does not include the item
* Chronicon does not record publish audit caused by denied operation

4. Denied content item update

Given authorized setup creates an item.

Unauthorized actor attempts update.

Assert:

* denied
* item content unchanged
* no update event emitted
* cache/index/audit not affected by denied update

## Important Testing Rule

Use `runtime.siteService()`, `runtime.contentTypeService()`, and `runtime.contentItemService()` for domain operations.

Do not perform domain operations through:

* `runtime.coreRuntime().siteService()`
* `runtime.coreRuntime().contentTypeService()`
* `runtime.coreRuntime().contentItemService()`

except for explicitly inspecting low-level state/projections/events where needed.

## Out of Scope

Do not implement:

* collection read filtering
* alias-to-SiteKey authorization
* restore/delete/unarchive/purge permission semantics
* direct actor grants
* explanation traces
* REST/Porta
* persistence/Archivum
* Olorin
* workflow
* new audit model for denied decisions

Do not rename:

* `ConciliumRuntime.inMemory()`
* `ConciliumRuntime.coreRuntime()`

## Acceptance Criteria

* Tests prove denied operations do not mutate state.
* Tests prove denied operations do not emit domain events.
* Tests prove denied publish does not update index.
* Tests prove denied operations do not create Chronicon audit records accidentally, if the current test surface allows observing this.
* Existing secured runtime tests continue passing.
* Existing unsecured runtime tests continue passing.
* No authorization semantics changed.
* No production code changed unless necessary to fix a discovered bug.

## Validation

Run:

* `git diff --check -- "*.java" "*.xml"`
* `mvn test -pl codex-concilium -am --no-transfer-progress`

If production code changes are made in Custos, also run:

* `mvn test -pl codex-custos,codex-concilium -am --no-transfer-progress`

## Deliverables

* tests added/updated
* implementation report under `docs/agents/reports/runtime/`
* validation output
* any discovered side-effect gaps
* any follow-up recommendations

## Architectural Notes

This task verifies the ordering guarantee:

```text
secured decorator
  -> permission decision
  -> requireGranted()
  -> delegate only if granted
  -> domain mutation
  -> event dispatch
  -> cache/index/audit/observance side effects
```

If authorization fails, the delegate must not be reached. Therefore downstream side effects must not occur.

This task does not design audit logging for denied decisions. That is a separate future task.
