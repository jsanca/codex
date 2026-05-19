# Deep Task — Observance Closure Review Plan Only

## Goal

Review the current Observance implementation after the timed service decorators and recent safe fixes.

Produce a plan/report only. Do not modify code.

## Scope

Review:

* `CodexRuntime` Observance propagation
* service decorators:

    * `TimedSiteService`
    * `TimedContentTypeService`
    * `TimedContentItemService`
* event dispatcher observance
* cache invalidation dispatcher observance
* index writer observance
* metric naming consistency
* no-op factory paths
* runtime integration tests
* remaining blind spots

## Questions to answer

1. Is the same runtime `Observance` instance propagated consistently?
2. Are there any places still using `Observance.noop()` accidentally?
3. Are metric names consistent across services, events, index, and cache?
4. Are there any cardinality risks?
5. Are tests sufficient to prove runtime observability?
6. What are the top 3 remaining Observance tasks?
7. Should we abstract repeated timed-service logic now, or wait?

## Constraints

* Do not edit production code.
* Do not edit tests.
* Do not introduce abstractions.
* Do not propose framework-heavy solutions.
* Keep recommendations incremental and aligned with ADR-008.

## Deliverable

A short report with:

* findings
* severity
* suggested next tasks
* any risks or inconsistencies
