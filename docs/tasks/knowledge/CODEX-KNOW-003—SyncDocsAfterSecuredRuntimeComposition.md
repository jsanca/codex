# Task CODEX-KNOW-003 — Sync docs after secured runtime composition

Status: Planned
Owner: Elito
Role: Knowledge Curator
Target: 20–30 minutes
Hard Stop: 45 minutes

## Execution Requirements

Apply, if available:

* `.opencode/skills/osk-engineering-reporting/SKILL.md`
* `.opencode/skills/osk-knowledge-curation/SKILL.md`

No commits.

This is documentation-only.

## Objective

Synchronize Codex documentation after completing Phase 3.1 Secured Runtime Composition.

## Context

CODEX-016 implemented secured runtime composition.

Completed:

* `ConciliumRuntime.secured(Supplier<PermissionResolutionSnapshot>)`
* `RuntimeSecurityMode.SECURED`
* `RuntimeSecurityMode.UNSECURED`
* secured service accessors:

    * `siteService()`
    * `contentTypeService()`
    * `contentItemService()`
* public `SecuredServiceComposer` in Custos API
* internal `Secured*Service` implementations remain hidden
* `codex-concilium` depends on `codex-custos`
* `codex-codex` remains pure
* `ConciliumRuntime.inMemory()` remains unsecured/back-compat
* `coreRuntime()` Javadoc now documents bypass risk

Deep accepted CODEX-016 with follow-up.
CODEX-016.1 completed that follow-up.

## Scope

Update documentation only.

Review and update as needed:

* `docs/CODEX-ROADMAP-PHASES.md`
* `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md`
* `docs/engineering/ENGINEERING_LOG.md`
* `docs/agents/checkpoints/` if a new checkpoint or checkpoint update is appropriate
* `docs/agents/README.md` only if runtime artifact discipline needs a small clarification

## Required Updates

1. Mark Phase 3.1 Secured runtime composition as DONE.

2. Document the resolved answers:

* secured runtime exists through `ConciliumRuntime.secured(...)`
* unsecured runtime remains available through `ConciliumRuntime.inMemory()`
* secured runtime is the recommended path for adapter/external/domain entrypoints
* unsecured runtime is a deliberate escape hatch for tests, low-level scenarios, and backward compatibility
* runtime metadata reports `SECURED` or `UNSECURED`
* metadata is diagnostic only; security is enforced by the exposed service graph

3. Document the composition rule:

* callers should use:

    * `runtime.siteService()`
    * `runtime.contentTypeService()`
    * `runtime.contentItemService()`

* callers should not use:

    * `runtime.coreRuntime().siteService()`
    * `runtime.coreRuntime().contentTypeService()`
    * `runtime.coreRuntime().contentItemService()`

for external/domain entrypoints, because those are raw core services.

4. Preserve accepted gaps:

* collection read filtering
* alias-to-SiteKey authorization
* restore/delete/unarchive/purge permission semantics
* audit/authorization integration
* denied-operation cache/index/Chronicon consistency expansion
* direct actor grants
* explanation trace

5. Add an engineering log entry for:

* CODEX-016 Secured Runtime Composition completed
* CODEX-016.1 coreRuntime bypass warning completed
* Phase 3.1 technically complete
* Recommended next technical task: CODEX-017 Denied-operation side-effect consistency

## Out of Scope

Do not:

* modify production code
* modify tests
* change runtime behavior
* rename `inMemory()`
* rename `coreRuntime()`
* introduce new ADRs unless a stale doc explicitly requires one
* resolve accepted technical gaps

## Validation

Run:

* `git diff --check -- docs`

No tests required because this is docs-only.

## Deliverables

* docs updated
* engineering log updated
* short report under `docs/agents/reports/knowledge/`
* any stale docs found but not changed
* no commits
