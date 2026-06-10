Task I — Introduce PermissionResolver

Context:
Custos now has the core Phase 1 data model:
- Permissions
- RoleKey
- Role
- PermissionGrant
- RoleAssignment
- BuiltInRoles

Now introduce PermissionResolver as the component that computes effective permissions from role assignments and role blueprints.

Scope:
Introduce PermissionResolver API and a minimal in-memory/default implementation if needed.

Do not introduce yet:
- Secured service decorators
- REST
- JWT
- SAML
- OIDC
- Spring Security
- servlet APIs
- persistence
- user management
- Olorin plan execution
- step-up approval

Design intent:
PermissionResolver computes effective permissions.
It does not return AccessDecision yet unless the existing API shape requires it.
It should not become a transport adapter.
It should not mutate assignments or roles.

Suggested concepts:
- PermissionResolver interface
- DefaultPermissionResolver or InMemoryPermissionResolver only if needed for tests
- EffectivePermission or ResolvedPermission only if the design truly needs it

Core behavior:
Given:
- Actor
- PermissionKey
- ResourceRef or ResourceScope target
- available RoleAssignments
- available Role blueprints
- available direct PermissionGrants if modeled

Resolve whether the actor effectively has the permission.

Rules to support eventually, but keep task small:
- RoleAssignment gives actor a RoleKey at a ResourceScope.
- RoleKey resolves to Role blueprint.
- Role permissions apply at the assignment scope.
- PermissionGrant represents PermissionKey + ResourceScope.
- contentItem.update implies contentItem.read.
- contentItem.publish implies contentItem.read.
- contentItem.publish does not imply contentItem.update.
- Site boundaries must not be crossed.

SUPER_ADMIN:
- Hard invariants must run before SUPER_ADMIN bypass.
- Agent actors must never exercise SUPER_ADMIN.
- If actor is AGENT and has SUPER_ADMIN, deny or surface invariant failure in evaluator/policy layer.
- For this resolver task, do not implement full policy engine unless explicitly scoped.
- Document where this guard belongs if not implemented yet.

Important:
Consider splitting into smaller tasks if PermissionResolver becomes too large:
1. API only
2. Role-based permission resolution
3. Scope hierarchy
4. Permission implications
5. SUPER_ADMIN bypass after hard rules
6. Explanation trace