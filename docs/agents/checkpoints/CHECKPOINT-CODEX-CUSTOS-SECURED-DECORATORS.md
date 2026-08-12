# Checkpoint: Codex Custos Secured Decorators

Date: 2026-08-05

## Summary

Custos Phase 1.4 secured service decorators are complete.

Codex now has secured decorators for the current Site, ContentType, and ContentItem
service surfaces, plus service-level authorization coverage for the real
ContentItem authorization chain.

Deep review confirmed Phase 1.4 as PASS with zero blockers.

This checkpoint preserves the architectural state, accepted decisions, temporary
gaps, and recommended next work after completing the secured decorator slice.

## Completed Components

Completed Custos components:

* `SecuredContentItemService`
* `SecuredContentTypeService`
* `SecuredSiteService`
* `SecuredContentItemServiceAuthorizationMatrixTest`
* domain permission services
* `AccessDecisionService` integration over `PermissionResolver`

Domain permission services currently include:

* `SitePermissionsService`
* `ContentTypePermissionsService`
* `ContentItemPermissionsService`
* `RoleAssignmentPermissionsService`

The secured decorators use these services rather than inspecting roles directly.

## Authorization Pattern

The secured decorators follow the same authorization pattern:

1. Validate required inputs with null guards.
2. Ask the matching domain permission service for an `AccessDecision`.
3. Call `AccessDecision.requireGranted()`.
4. Delegate to the underlying Codex service only after a granted decision.
5. Ensure denied decisions do not reach the delegate.
6. Allow invariant exceptions to propagate uncaught.

Important invariant behavior:

* `AccessDeniedException` represents a normal denied authorization decision.
* `CustosAgentSuperAdminInvariantViolationException` is not converted to denied.
* Hard invariant violations remain fatal and propagate through secured decorators.

## Module Boundary Decision

Secured decorators live in `codex-custos` internal service code.

This preserves the dependency direction:

```text
codex-custos -> codex-codex -> codex-fundamentum
```

`codex-codex` remains independent of `codex-custos`.

The core domain kernel does not import Custos types and does not know whether a
caller is using secured decorators, unsecured services, tests, or a future secure
runtime composition.

## Forwarding Service Decision

`codex-codex` has internal `Forwarding*Service` interfaces used by core service
decorators.

Custos secured decorators intentionally do not use them.

Reason:

* forwarding services delegate by default
* security decorators must decide each method explicitly
* accidental delegation is unsafe for authorization boundaries

Every secured method must choose one of three postures:

* authorize before delegating
* fail closed
* documented temporary pass-through

This explicit method-by-method posture is part of the security design.

## Fail-Closed Decisions

The following operations fail closed:

* `ContentItem` delete
* `ContentItem` restore
* `Site` unarchive

These operations throw `UnsupportedOperationException` in their secured decorators
instead of delegating.

No premature permission vocabulary was introduced for:

* content item delete
* content item restore
* purge
* site unarchive

This keeps sensitive lifecycle and retention semantics out of the permission model
until the domain vocabulary is intentionally defined.

## Documented Pass-Through Gaps

The following methods are currently documented pass-through operations:

* `ContentItem` `findByContentType`
* `ContentItem` `findAll`
* `ContentType` `findBySiteKey`
* `ContentType` `findAll`
* `Site` `findByAlias`
* `Site` `findAll`

These are accepted temporary gaps.

They remain pending because collection reads need a filtering strategy:

* per-item filtering
* query-level filtering
* pagination-aware authorization behavior

`Site.findByAlias` also needs an alias-to-`SiteKey` authorization strategy before
it can check `canReadSite` without changing lookup semantics.

The current pass-through methods are documented in code and tests so they remain
visible rather than accidental.

## Authorization Matrix Coverage

`SecuredContentItemServiceAuthorizationMatrixTest` exercises the full real Custos
chain:

```text
BuiltInRoles
    -> RoleAssignment
    -> PermissionResolver
    -> AccessDecisionService
    -> ContentItemPermissionsService
    -> SecuredContentItemService
```

Covered scenarios include:

* viewer role behavior
* copywriter role behavior
* reviewer role behavior
* editor role behavior
* site boundary behavior
* AGENT + SUPER_ADMIN hard invariant behavior

This test is valuable because it verifies secured service behavior through the
actual authorization chain instead of only through stubs.

## Accepted Gaps

Accepted gaps after Phase 1.4:

* secured runtime composition is not done
* collection read filtering is not done
* alias-to-`SiteKey` authorization is not done
* content item delete/restore permission semantics are not defined
* site unarchive permission semantics are not defined
* restore/purge/archive administration vocabulary still needs deeper design
* denied-operation cache/index/event consistency tests remain future work
* audit/authorization integration remains future work

These gaps are accepted because Phase 1.4's goal was secured decorator coverage,
not final runtime composition or full read-filtering semantics.

## Known Future Work

Recommended future work:

* secure runtime composition
* collection read filtering
* alias resolution before authorization
* archive/restore/purge semantics
* audit/authorization integration
* cache/index consistency tests under denied operations

Additional design topics likely need ADR-level treatment:

* whether secure runtime composition is default
* how permission snapshots are supplied at runtime
* how list filtering interacts with pagination
* how denied decisions are recorded, observed, or audited
* how retention/archive administration maps to permissions

## Recommended Next Task

Recommended next task:

```text
Phase 3.1 Secured runtime composition
```

Before runtime composition, the team may optionally run one final docs sync review
to confirm that:

* `CODEX-ROADMAP-PHASES.md`
* `CUSTOS-IMPLEMENTATION-CHECKLIST.md`
* `CODEX-BLUEPRINT.md`
* `AGENTS.md`
* `CLAUDE.md`

all describe the same Custos state.

## Resume Point

If work resumes from this checkpoint, start by deciding whether to:

1. run a final docs sync review, or
2. begin Phase 3.1 secured runtime composition.

Do not start by adding new permission keys for delete, restore, purge, or
unarchive. Those semantics remain intentionally deferred.
