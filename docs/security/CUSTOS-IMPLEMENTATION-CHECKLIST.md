# Custos Implementation Checklist

This checklist tracks the Custos roadmap from ADR-009. It is an implementation guide, not a replacement for the ADR.

Custos remains a domain authorization kernel:

```text
Actor + Permission + Resource + Context -> AccessDecision
```

Authentication, transport adapters, persistence, REST, JWT, SAML, OIDC, Spring Security, servlet APIs, and user management remain out of scope for this roadmap stage.

## Status

- [x] Phase 0 primitives implemented.
- [ ] Phase 0 pending review after Clio Task C.
- [ ] Phase 0 hardening active.
- [ ] Phase 1 not started.
- [ ] Phase 2 not started.
- [ ] Phase 3 not started.
- [ ] Phase 4 not started.

## Current Active Task: Phase 0 Hardening

Phase 0 exists in `codex-custos` and should be hardened before adding roles, grants, permission resolution, or secured service decorators.

- [ ] Harden `PermissionKey` validation.
- [ ] Harden `ResourceRef` invariants.
- [ ] Harden `ResourceScope` invariants.
- [ ] Confirm `AccessDecision` behavior and denial handling.
- [ ] Confirm `SecurityEvaluationContext` null and empty handling.
- [ ] Confirm `PermissionEvaluator` and `AccessDecisionService` API alignment with ADR-009.
- [ ] Confirm module hygiene remains transport-agnostic and persistence-free.

Do not add Phase 1 concepts during Phase 0 hardening.

## Phase 0: Authorization Kernel

Implemented, pending review after Clio Task C.

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

## Phase 1: Roles and Grants

Not started.

- [ ] `Role`
- [ ] `RoleKey`
- [ ] `RoleAssignment`
- [ ] `PermissionGrant`
- [ ] `PermissionResolver`
- [ ] `BuiltInRoles`

## Phase 2: Domain Permission Services

Not started.

- [ ] `SitePermissionsService`
- [ ] `ContentTypePermissionsService`
- [ ] `ContentItemPermissionsService`
- [ ] `RoleAssignmentPermissionsService`

## Phase 3: Secured Service Decorators

Not started.

- [ ] `SecuredSiteService`
- [ ] `SecuredContentTypeService`
- [ ] `SecuredContentItemService`

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

`PermissionKey` names a domain permission such as a content or site action. `RoleKey` will name a role in Phase 1; roles are not part of Phase 0 hardening.

### ResourceRef vs ResourceScope

`ResourceRef` points to the concrete resource being evaluated. `ResourceScope` represents where a permission or role is assigned.

### AccessDecision vs boolean

`AccessDecision` explains the authorization result and carries context such as actor, permission, resource, and denial reason. A boolean only answers yes or no and is not enough for Custos decisions.

### PermissionEvaluator vs Domain Permission Services

`PermissionEvaluator` is the low-level evaluation port. Domain permission services such as `SitePermissionsService` and `ContentItemPermissionsService` belong to Phase 2 and should express domain-specific authorization checks.

### Olorin Proposer vs Executor

Olorin may propose or prepare permission changes. Olorin must not execute privileged permission changes by its own authority.

## Open Questions

- What exact review checklist should close Clio Task C?
- Which Phase 1 type should be introduced first after Phase 0 hardening is accepted?
