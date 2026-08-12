# Knowledge Reconciliation Report — CODEX-KNOW-004

## Task

`TaskCODEX-KNOW-004—ReconcileStaleCustosModelDocs`

## Date

2026-08-08

## Owner

Elito

## Mode

Authorized reconciliation. Documentation-only.

## Scope

Updated stale Custos model documentation after CODEX-016, CODEX-017, and CODEX-018.

## Files Changed

* `docs/security/CUSTOS-MODEL.md`
* `docs/agents/reports/knowledge/CODEX-KNOW-004—ReconcileStaleCustosModelDocs—REPORT.md`

## Evidence Reviewed

* `docs/tasks/knowledge/TaskCODEX-KNOW-004—ReconcileStaleCustosModelDocs.md`
* `docs/agents/reports/runtime/CODEX-016-SecuredRuntimeComposition-REPORT.md`
* `docs/agents/reports/runtime/CODEX-017-DeniedSideEffectConsistency-REPORT.md`
* `docs/agents/reports/knowledge/CODEX-018—AuthorizationAuditAndDecisionTraceDiscovery—REPORT.md`
* `docs/CODEX-ROADMAP-PHASES.md`
* `AGENTS.md`
* `CLAUDE.md`
* `codex-custos/src/main/java/codex/custos/api/service/AccessDecisionService.java`
* `codex-custos/src/main/java/codex/custos/internal/service/DefaultAccessDecisionService.java`
* `codex-custos/src/main/java/codex/custos/api/service/SecuredServiceComposer.java`
* `codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java`

## Reconciliations

| Finding | Classification | Change |
| --- | --- | --- |
| `CUSTOS-MODEL.md` said `AccessDecisionService` wiring was pending. | Stale | Replaced with current flow: `PermissionResolver` produces `PermissionResolution`; `DefaultAccessDecisionService` maps it to `AccessDecision`. |
| `CUSTOS-MODEL.md` did not describe implemented domain permission services. | Missing | Added `SitePermissionsService`, `ContentTypePermissionsService`, and `ContentItemPermissionsService` section. |
| `CUSTOS-MODEL.md` did not describe secured decorators as implemented. | Missing | Added `SecuredSiteService`, `SecuredContentTypeService`, and `SecuredContentItemService` section. |
| `CUSTOS-MODEL.md` did not describe secured runtime composition. | Missing | Added `ConciliumRuntime.secured(...)`, unsecured `inMemory()`, and raw `coreRuntime()` rule. |
| `CUSTOS-MODEL.md` did not include CODEX-018 audit/trace direction. | Missing | Added authorization audit direction: denied attempts are security facts, Chronicon domain audit stays clean, Observance metrics are aggregate/PI-safe, logs are diagnostic, decision records/consumers are future work. |

## Current-State Claims Preserved

* `Role` remains a blueprint and does not carry an actor or scope.
* `RoleAssignment` connects actor + role key + scope.
* `PermissionGrant` remains `PermissionKey + ResourceScope` and does not carry an actor or role.
* Scope hierarchy and permission implication remain resolver/evaluator behavior, not data-record behavior.
* Direct actor grants remain pending.
* `AccessDecision` trace remains pending.
* Security audit consumers/sinks remain pending.

## Optional Docs

No optional docs were changed. `docs/CODEX-ROADMAP-PHASES.md`, `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md`, and `docs/engineering/ENGINEERING_LOG.md` already matched the current state closely enough for this task's scope.

## Validation

```bash
git diff --check -- docs
```

Result: passed.

No tests were run because this was documentation-only.

## Remaining Stale Items Found

Production JavaDoc in `RoleAssignment` and `PermissionGrant` still uses future-facing wording
around `PermissionResolver`. Those files are production code and were out of scope for this
documentation-only task.

## No Production Changes

No production code, tests, module descriptors, runtime wiring, or authorization semantics were
changed.
