# Deep Review — CODEX-016 Secured Runtime Composition

Status: Planned
Owner: Deep
Role: Reviewer
Target: 20–30 minutes
Hard Stop: 45 minutes

## Execution Requirements

Apply, if available:

* `.opencode/skills/architecture/osk-architecture-review/SKILL.md`
* `.opencode/skills/process/osk-engineering-reporting/SKILL.md`

No commits.

Create review report under:

* `docs/agents/reviews/runtime/`

## Objective

Review CODEX-016 Secured Runtime Composition and verify that the secured runtime path is architecturally safe, behaviorally enforced, and does not introduce dependency inversion or security bypass risks.

## Context

Clio implemented Phase 3.1 using approved Option B:

* `SecuredServiceComposer` public factory in `codex.custos.api.service`
* `Secured*Service` implementations remain internal
* `codex-concilium` depends on `codex-custos`
* `codex-codex` remains pure
* `ConciliumRuntime.secured(Supplier<PermissionResolutionSnapshot>)` added
* `ConciliumRuntime.inMemory()` preserved as unsecured/back-compat
* runtime metadata added via `RuntimeSecurityMode.SECURED` / `UNSECURED`

Implementation report:

* `docs/agents/reports/runtime/CODEX-016-SecuredRuntimeComposition-REPORT.md`

## Review Scope

Inspect:

* `codex-custos/src/main/java/codex/custos/api/service/SecuredServiceComposer.java`
* `codex-custos/src/main/java/module-info.java`
* `codex-custos/src/main/java/codex/custos/internal/service/SecuredSiteService.java`
* `codex-custos/src/main/java/codex/custos/internal/service/SecuredContentTypeService.java`
* `codex-custos/src/main/java/codex/custos/internal/service/SecuredContentItemService.java`
* `codex-concilium/pom.xml`
* `codex-concilium/src/main/java/module-info.java`
* `codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java`
* `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeSecuredTest.java`
* relevant runtime tests
* `docs/agents/tasks/runtime/Task-CODEX-016—SecuredRuntimeComposition.md`
* `docs/agents/reports/runtime/CODEX-016-SecuredRuntimeComposition-REPORT.md`

## Review Questions

1. Does `codex-codex` remain independent of `codex-custos`?
2. Does `codex-concilium` depend on `codex-custos` without creating cycles?
3. Does `codex-custos` avoid exporting `codex.custos.internal.service`?
4. Is `SecuredServiceComposer` a thin public bridge with zero authorization logic?
5. Does `ConciliumRuntime.secured(...)` expose secured services through:

    * `runtime.siteService()`
    * `runtime.contentTypeService()`
    * `runtime.contentItemService()`
6. Does `ConciliumRuntime.inMemory()` still behave as unsecured/back-compat?
7. Is `RuntimeSecurityMode` diagnostic only, not used as a security gate?
8. Is `snapshotProvider` preserved as per-operation supplier and not evaluated eagerly at runtime construction?
9. Do behavioral tests prove unauthorized actors are denied?
10. Do behavioral tests prove denied operations do not mutate state?
11. Do behavioral tests prove denied operations do not emit events?
12. Do authorized operations still preserve event/index/Chronicon behavior?
13. Does AGENT + SUPER_ADMIN invariant propagate through secured runtime?
14. Is `coreRuntime().siteService()` bypass risk documented and acceptable for now?
15. Should `coreRuntime()` remain publicly exposed on secured runtimes, or should this become a follow-up hardening task?

## Special Attention: Bypass Risk

The implementation report states:

* `runtime.siteService()` returns secured service in secured runtime.
* `runtime.coreRuntime().siteService()` still returns raw service.

Review whether this is acceptable as an explicit internal escape hatch, or whether it should be hardened before Phase 3.1 is considered complete.

Consider possible follow-ups:

* Rename accessor to make raw access explicit.
* Document `coreRuntime()` as unsafe/raw.
* Add warning Javadocs.
* Hide raw runtime from secured runtime in future.
* Add test/documentation asserting adapters must use runtime service accessors, not `coreRuntime()`.

## Output Format

Produce:

* PASS / WARN / BLOCKER summary
* module boundary assessment
* runtime composition assessment
* security bypass assessment
* test quality assessment
* accepted gaps
* required fixes, if any
* recommendation:

    * CODEX-016 accepted
    * CODEX-016 accepted with follow-up
    * CODEX-016 requires fixes before acceptance

## Validation

Run if possible:

* `mvn test -pl codex-custos,codex-concilium -am --no-transfer-progress`
* `git diff --check`

If not run, state that validation was static only.

## Definition of Done

* Review report created under `docs/agents/reviews/runtime/`
* PASS/WARN/BLOCKER clearly stated
* Bypass risk explicitly assessed
* No code modified
* No commits performed
