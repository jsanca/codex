# Task CODEX-KNOW-004 — Reconcile stale Custos model docs

Status: Planned
Owner: Elito
Role: Knowledge Curator
Target: 20–30 minutes
Hard Stop: 45 minutes

## Execution Requirements

Apply, if available:

* `.opencode/skills/osk-knowledge-curation/SKILL.md`
* `.opencode/skills/osk-engineering-reporting/SKILL.md`

No commits.

This is documentation-only.

## Objective

Reconcile stale Custos model documentation after CODEX-016, CODEX-017, and CODEX-018.

## Context

CODEX-018 discovery found that:

* `docs/security/CUSTOS-MODEL.md` still says `AccessDecisionService` is pending.
* This is stale because Custos now has a completed authorization loop, secured decorators, secured runtime composition, and denied-operation side-effect consistency tests.

Current real state:

* `AccessDecisionService` exists.
* `DefaultAccessDecisionService` delegates to `PermissionResolver`.
* Domain permission services exist:

    * `SitePermissionsService`
    * `ContentTypePermissionsService`
    * `ContentItemPermissionsService`
* Secured decorators exist:

    * `SecuredSiteService`
    * `SecuredContentTypeService`
    * `SecuredContentItemService`
* `ConciliumRuntime.secured(...)` composes secured services.
* `ConciliumRuntime.inMemory()` remains unsecured/back-compat.
* Denied operations do not mutate state, emit events, update index, or create Chronicon records accidentally.
* CODEX-018 proposes authorization decision records/events and configurable consumers/sinks as future direction.

## Scope

Update:

* `docs/security/CUSTOS-MODEL.md`

Optionally update only if directly referenced/stale:

* `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md`
* `docs/CODEX-ROADMAP-PHASES.md`
* `docs/engineering/ENGINEERING_LOG.md`

Create report under:

* `docs/agents/reports/knowledge/`

## Required Updates

1. Remove or update stale language saying `AccessDecisionService` is pending.

2. Describe current authorization flow:

```text
Actor
  -> RoleAssignment
  -> Role
  -> PermissionGrant
  -> PermissionResolver
  -> PermissionResolution
  -> AccessDecisionService
  -> AccessDecision
  -> secured service decorator
```

3. Document domain permission services as implemented.

4. Document secured decorators as implemented.

5. Document secured runtime composition:

```text
ConciliumRuntime.secured(...)
  -> exposes secured SiteService
  -> exposes secured ContentTypeService
  -> exposes secured ContentItemService
```

6. Document raw/unsecured runtime rule:

* `ConciliumRuntime.inMemory()` remains unsecured.
* `coreRuntime()` exposes raw core runtime.
* external/domain entrypoints should use top-level runtime service accessors.

7. Document accepted gaps:

* collection read filtering
* alias-to-SiteKey authorization
* restore/delete/unarchive/purge permission vocabulary
* direct actor grants
* AccessDecision trace
* security audit consumers/sinks

8. Mention CODEX-018 direction:

* denied authorization attempts are security facts, not domain facts
* Chronicon domain audit stream should remain clean
* possible future Chronicon security audit stream
* Observance receives PI-safe aggregate metrics
* logs remain diagnostic
* AccessDecision may become source of decision records/events

## Out of Scope

Do not:

* modify production code
* modify tests
* implement audit events
* implement AccessDecision trace
* implement security audit stream
* change authorization semantics
* change runtime composition

## Validation

Run:

* `git diff --check -- docs`

No tests required.

## Deliverables

* updated `CUSTOS-MODEL.md`
* short report under `docs/agents/reports/knowledge/`
* any additional stale docs found
* no commits
