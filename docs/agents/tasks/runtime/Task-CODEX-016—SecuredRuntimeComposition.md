# Task CODEX-016 — Secured Runtime Composition

## Task ID

`CODEX-016`

## Title

Secured Runtime Composition

## Status

`READY`

## Owner

Clio

## Role

Implementer

## Target Time

One focused implementation slice.

## Hard Stop

Stop before changing authorization semantics, REST/Porta, persistence, workflow,
collection read filtering, alias authorization, or retention vocabulary.

## Execution Requirements

* Apply `.opencode/skills/architecture/osk-architecture-review/SKILL.md` if available.
* Apply `.opencode/skills/process/osk-engineering-reporting/SKILL.md` if available.
* No commits.
* Create or update a checkpoint after implementation.
* Preserve existing unsecured runtime paths for backward compatibility.

## Objective

Implement Phase 3.1 Secured Runtime Composition using approved Option B.

The goal is to allow Concilium to expose an explicit secured runtime path while
keeping `codex-codex` pure and keeping secured service implementations internal to
Custos.

## Context

Custos Phase 1.4 secured service decorators are complete:

* `SecuredContentItemService`
* `SecuredContentTypeService`
* `SecuredSiteService`
* domain permission services
* `AccessDecisionService` integration over `PermissionResolver`
* service-level ContentItem authorization matrix coverage

The current checkpoint is:

```text
docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md
```

Deep's discovery for Phase 3.1 approved Option B:

* Custos exposes public `SecuredServiceComposer` in `codex.custos.api.service`.
* `Secured*Service` implementations remain internal.
* Concilium depends on Custos.
* `codex-codex` remains pure.
* `ConciliumRuntime.secured(Supplier<PermissionResolutionSnapshot>)` exposes secured services.
* `ConciliumRuntime.inMemory()` remains explicit unsecured/back-compat path.
* Runtime metadata should indicate whether the runtime is `SECURED` or `UNSECURED`.

## Scope

Included:

* add public Custos composition surface for secured services
* keep secured decorator implementations internal
* wire explicit secured composition through Concilium
* expose secured services from the secured runtime path
* preserve unsecured `ConciliumRuntime.inMemory()` behavior
* add runtime metadata indicating `SECURED` or `UNSECURED`
* add focused tests for secured vs unsecured composition behavior

## Out Of Scope

Do not implement:

* REST/Porta
* Archivum/persistence
* Olorin
* workflow
* collection read filtering
* alias-to-SiteKey authorization
* restore/purge/unarchive semantics
* changes to Custos authorization semantics
* renaming `ConciliumRuntime.inMemory()`

## Acceptance Criteria

* `codex-codex` does not depend on `codex-custos`.
* `codex-custos` exposes a public composition entry point for secured services.
* `Secured*Service` implementations remain internal.
* Concilium can create an explicit secured runtime path.
* `ConciliumRuntime.inMemory()` remains unsecured and backward compatible.
* Runtime metadata distinguishes `SECURED` from `UNSECURED`.
* Secured runtime services enforce Custos decorators.
* Unsecured runtime services preserve current behavior.
* Tests cover secured and unsecured composition paths.

## Deliverables

* production changes for Custos and Concilium composition
* tests proving secured/unsecured runtime composition behavior
* implementation report
* checkpoint created or updated after implementation

## Architectural Notes

Option B preserves the core dependency rule:

```text
codex-concilium -> codex-custos -> codex-codex -> codex-fundamentum
```

`codex-codex` remains the domain kernel and must not import Custos.

Custos owns authorization decorators and secured service composition.

Concilium owns runtime assembly and may depend on Custos for explicit secured
composition.

## Definition Of Done

* implementation scope complete
* tests added or updated
* validation commands run and reported
* no out-of-scope semantics changed
* implementation report produced
* checkpoint created or updated

## Validation

Required:

```bash
git diff --check
mvn test -pl codex-custos,codex-concilium -am --no-transfer-progress
```

Run broader validation if changes affect shared runtime behavior.

## Checkpoint Requirement

Create or update a checkpoint after implementation.

Recommended checkpoint path:

```text
docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-RUNTIME-COMPOSITION.md
```
