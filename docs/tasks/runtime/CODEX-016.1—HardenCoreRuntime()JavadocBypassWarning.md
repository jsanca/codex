# Task CODEX-016.1 — Harden coreRuntime() Javadoc Bypass Warning

Status: Planned
Owner: Clio
Role: Implementation
Target: 10–15 minutes
Hard Stop: 25 minutes

## Objective

Document the bypass risk of `ConciliumRuntime.coreRuntime()` on secured runtimes.

## Context

CODEX-016 implemented secured runtime composition and Deep accepted it with follow-up.

The secured runtime exposes authorization-enforcing services through:

* `runtime.siteService()`
* `runtime.contentTypeService()`
* `runtime.contentItemService()`

However:

* `runtime.coreRuntime().siteService()`
* `runtime.coreRuntime().contentTypeService()`
* `runtime.coreRuntime().contentItemService()`

still return raw core services and bypass Custos authorization.

This is acceptable for now, but must be explicitly documented.

## Scope

Update only:

* `codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java`

## Required Change

Update the Javadoc of `coreRuntime()` to explain:

* it returns the raw core runtime
* on secured Concilium runtimes, raw services obtained through `coreRuntime()` bypass Custos authorization
* domain callers, adapters, Porta, Olorin, and external entry points should use:

    * `siteService()`
    * `contentTypeService()`
    * `contentItemService()`
* `coreRuntime()` exists for lower-level runtime concerns such as projections, recorded events, lifecycle, tests, and internal composition

## Out of Scope

Do not:

* rename `coreRuntime()`
* hide `coreRuntime()`
* rename `inMemory()`
* change runtime behavior
* change security semantics
* modify Custos
* modify tests unless strictly necessary

## Validation

Run:

* `git diff --check -- "*.java"`
* `mvn test -pl codex-concilium -am --no-transfer-progress`

## Deliverables

* Javadoc updated
* short implementation report under `docs/agents/reports/runtime/`
* validation results
* no commits
