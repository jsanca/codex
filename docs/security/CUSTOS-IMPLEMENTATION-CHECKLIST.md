# Custos Implementation Checklist

This checklist tracks the Custos roadmap from ADR-009. It is an implementation guide, not a replacement for the ADR.

Custos remains a domain authorization kernel:

```text
Actor + Permission + Resource + Context -> AccessDecision
```

Authentication, transport adapters, persistence, REST, JWT, SAML, OIDC, Spring Security, servlet APIs, and user management remain out of scope for this roadmap stage.

## Status

- [x] Phase 0 primitives implemented.
- [x] Phase 0 hardened and reviewed after Clio Task C.
- [x] Permissions catalog added.
- [x] Phase 1 roles and resolver primitives implemented.
- [x] AccessDecisionService wiring over PermissionResolver implemented.
- [x] Domain permission services implemented.
- [x] ContentType lifecycle permission vocabulary aligned to archive.
- [x] SecuredContentItemService implemented.
- [x] Service-level authorization matrix test added.
- [x] Phase 2 domain permission services complete.
- [x] Phase 3 secured service decorators started.
- [ ] Phase 4 not started.

## Phase 0 Hardening

Complete and reviewed.

- [x] Harden `PermissionKey` validation.
- [x] Harden `ResourceRef` invariants.
- [x] Harden `ResourceScope` invariants.
- [x] Confirm `AccessDecision` behavior and denial handling.
- [x] Confirm `SecurityEvaluationContext` null and empty handling.
- [x] Confirm `PermissionEvaluator` and `AccessDecisionService` API alignment with ADR-009.
- [x] Confirm module hygiene remains transport-agnostic and persistence-free.

Phase 1 work may proceed from these hardened primitives.

## Phase 0: Authorization Kernel

Implemented and reviewed.

- [x] `Actor`
- [x] `ActorId`
- [x] `ActorType`
- [x] `PermissionKey`
- [x] `ResourceRef`
- [x] `ResourceScope`
- [x] `AccessDecision`
- [x] `AccessDeniedException`
- [x] `SecurityEvaluationContext`
- [x] `PermissionEvaluator`
- [x] `AccessDecisionService`

## Permissions Catalog

Added.

- [x] `Permissions`

## Phase 1: Roles and Grants

Started.

- [x] `RoleKey`
- [x] `Role`
- [x] `PermissionGrant`
- [x] `RoleAssignment`
- [x] `BuiltInRoles`
- [x] `PermissionResolver`

Phase 1 notes:

- `RoleKey` names a role.
- `Role` is a blueprint for permissions.
- `Role` must not contain an `Actor`.
- `Role` must not contain a `ResourceScope`.
- `PermissionGrant` pairs a `PermissionKey` with a `ResourceScope`. It does not carry an actor or role.
- `RoleAssignment` connects an actor to a role at a scope.
- `BuiltInRoles` is a catalog of `Role` blueprints.
- `SUPER_ADMIN` includes all built-in permissions for introspection and blueprint purposes.
- `SUPER_ADMIN` bypass logic is not encoded in `BuiltInRoles`; it belongs in resolver/evaluator behavior.
- Hard invariants must run before any `SUPER_ADMIN` bypass.
- `PermissionResolver` API exists.
- `DefaultPermissionResolver` exists as the internal implementation.
- `DefaultPermissionResolver` computes effective permissions from `RoleAssignment` plus the `Role` registry.
- `DefaultPermissionResolver` enforces the AGENT + SUPER_ADMIN hard invariant before bypass.
- `DefaultPermissionResolver` applies scoped `SUPER_ADMIN` bypass for valid non-agent actors.
- `DefaultPermissionResolver` walks the scope hierarchy without crossing site boundaries.
- `DefaultPermissionResolver` implements `contentItem.update` -> `contentItem.read` and `contentItem.publish` -> `contentItem.read` implications.

Phase 1 pending follow-up:

- [ ] Direct actor `PermissionGrant` support. DEFERRED
- [ ] Explanation trace.
- [x] `AccessDecisionService` wiring over `PermissionResolver`.

## Phase 2: Domain Permission Services

Implemented.

- [x] `SitePermissionsService`
- [x] `ContentTypePermissionsService`
- [x] `ContentItemPermissionsService`
- [x] `RoleAssignmentPermissionsService`

### ContentType Lifecycle Vocabulary Alignment

Done.

- [x] `CONTENT_TYPE_DELETE` removed.
- [x] `contentType.delete` removed.
- [x] `canDeleteContentType` removed.
- [x] `CONTENT_TYPE_ARCHIVE` added.
- [x] `contentType.archive` added.
- [x] `canArchiveContentType` added.

## Phase 3: Secured Service Decorators

Phase 1.4 secured decorators are done.

- [x] `SecuredSiteService`
- [x] `SecuredContentTypeService`
- [x] `SecuredContentItemService`
- [x] `SecuredContentItemServiceAuthorizationMatrixTest`

Secured decorator notes:

- ContentItem create/findByKey/update/publish/unpublish/archive are gated.
- ContentType keyed read and mutating operations are gated.
- Site keyed read and lifecycle operations are gated.
- ContentItem delete/restore are fail-closed until permission semantics are defined.
- Site unarchive is fail-closed until permission semantics are defined.
- ContentItem findByContentType/findAll remain pass-through pending read filtering strategy.
- ContentType findBySiteKey/findAll remain pass-through pending read filtering strategy.
- Site findByAlias/findAll remain pass-through pending read filtering strategy.
- alias-to-SiteKey authorization remains pending/future.
- secure runtime composition is done through `ConciliumRuntime.secured(...)`.
- restore/purge/unarchive permission semantics remain pending/future.
- SecuredContentItemServiceAuthorizationMatrixTest uses the real Custos chain with `BuiltInRoles`, `RoleAssignment`, `PermissionResolver`, `AccessDecisionService`, `ContentItemPermissionsService`, and `SecuredContentItemService`.
- The authorization matrix covers viewer, copywriter, reviewer, editor, site boundary, and AGENT + SUPER_ADMIN invariant scenarios.

Secured runtime composition notes:

- `ConciliumRuntime.secured(...)` exposes authorization-enforcing services.
- `ConciliumRuntime.inMemory()` remains an explicit unsecured/back-compat path.
- secured runtimes report `SECURED`; unsecured runtimes report `UNSECURED`.
- runtime metadata is diagnostic only; security is enforced by the exposed service graph.
- adapter, external, and domain entrypoints should call `runtime.siteService()`, `runtime.contentTypeService()`, and `runtime.contentItemService()`.
- callers should not use `runtime.coreRuntime().siteService()`, `runtime.coreRuntime().contentTypeService()`, or `runtime.coreRuntime().contentItemService()` for adapter, external, or domain entrypoints.

## Phase 4: Permission Change Workflow

Not started.

- [ ] `PermissionChangePlan`
- [ ] `StepUpApproval`
- [ ] Olorin proposal flow
- [ ] Audit integration

## Glossary

### Actor vs User

`Actor` is the internal Codex subject used for authorization decisions. A `User` is a possible human identity behind an actor, but Custos Phase 0 does not implement user management.

### PermissionKey vs RoleKey

`PermissionKey` names a domain permission such as a content or site action. `RoleKey` names a role.

### ResourceRef vs ResourceScope

`ResourceRef` points to the concrete resource being evaluated. `ResourceScope` represents where a permission or role is assigned.

### AccessDecision vs boolean

`AccessDecision` explains the authorization result and carries context such as actor, permission, resource, and denial reason. A boolean only answers yes or no and is not enough for Custos decisions.

### PermissionEvaluator vs Domain Permission Services

`PermissionEvaluator` is the low-level evaluation port. Domain permission services such as `SitePermissionsService` and `ContentItemPermissionsService` belong to Phase 2 and should express domain-specific authorization checks.

### Olorin Proposer vs Executor

Olorin may propose or prepare permission changes. Olorin must not execute privileged permission changes by its own authority.

## Open Questions

- Which pending Phase 1 follow-up should come first: direct grants, explanation trace, or `AccessDecisionService` wiring?
