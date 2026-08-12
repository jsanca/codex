# Deep Review — CODEX-017 Denied-Operation Side-Effect Consistency

Status: Planned
Owner: Deep
Role: Reviewer
Target: 20–30 minutes
Hard Stop: 45 minutes

## Execution Requirements

Apply, if available:

* `.opencode/skills/osk-architecture-review/SKILL.md`
* `.opencode/skills/osk-engineering-reporting/SKILL.md`

No commits.

Create review report under:

* `docs/agents/reviews/runtime/`

## Objective

Review CODEX-017 and verify that denied operations through `ConciliumRuntime.secured(...)` produce no unintended side effects across domain state, events, Chronicon audit records, and index projections.

## Context

Clio implemented CODEX-017 as tests-only.

New test class:

* `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeDeniedSideEffectsTest.java`

Reported coverage:

* Denied site create
* Denied content type create
* Denied content item publish
* Denied content item update

Reported validation:

* `mvn test -pl codex-concilium -am --no-transfer-progress`
* 57 tests, 0 failures, BUILD SUCCESS

No production code changed.

## Review Scope

Inspect:

* `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeDeniedSideEffectsTest.java`
* `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeSecuredTest.java`
* `codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java`
* relevant CodexRuntime/event/Chronicon/index test surfaces
* `docs/agents/reports/runtime/CODEX-017-DeniedSideEffectConsistency-REPORT.md`

## Review Questions

1. Do denied operations go through top-level secured runtime accessors:

    * `runtime.siteService()`
    * `runtime.contentTypeService()`
    * `runtime.contentItemService()`

2. Are raw `runtime.coreRuntime()` services used only for low-level state inspection?

3. Does denied site create prove:

    * AccessDeniedException
    * no site state mutation
    * no recorded domain events
    * no Chronicon audit records

4. Does denied content type create prove:

    * AccessDeniedException
    * no content type state mutation
    * no new domain events
    * no new Chronicon audit records

5. Does denied content item publish prove:

    * AccessDeniedException
    * item remains draft
    * no published revision
    * no new domain events
    * no Chronicon audit records
    * no index update

6. Does denied content item update prove:

    * AccessDeniedException
    * working revision/content remains unchanged
    * no new events
    * no new audit records

7. Is the index assertion strong enough?

    * Clio reported that index updates are event-driven, so unchanged events imply no index subscriber invocation.
    * Verify whether this inference is valid in current Concilium/IndexRuntime design.
    * If a direct index read is available and cheap, recommend adding it.
    * If not, document the inference as acceptable.

8. Are baselines captured after authorized setup and before denied action?

9. Are tests behavior-focused rather than coupled to internals?

10. Did Clio avoid production code changes?

11. Did existing secured/unsecured runtime behavior remain intact?

12. Are any accepted gaps accidentally expanded?

## Output Format

Produce:

* PASS / WARN / BLOCKER summary
* behavior coverage assessment
* side-effect coverage assessment
* index observability assessment
* test quality assessment
* any required fixes
* any follow-up recommendations
* acceptance recommendation:

    * accept CODEX-017
    * accept with follow-up
    * require fixes

## Validation

Run if possible:

* `git diff --check -- "*.java" "*.xml"`
* `mvn test -pl codex-concilium -am --no-transfer-progress`

If not run, state that validation was static only.

## Definition of Done

* Review report created under `docs/agents/reviews/runtime/`
* Bypass/coreRuntime usage assessed
* Index inference assessed
* No code modified
* No commits performed
