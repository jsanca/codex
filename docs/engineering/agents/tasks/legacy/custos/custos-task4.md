Task F — Introduce PermissionGrant

Context:
Custos Phase 0 is complete and hardened. The Permissions catalog exists. RoleKey and Role now exist as the first Phase 1 primitives.

We now need the next Phase 1 primitive: PermissionGrant.

Scope:
Introduce PermissionGrant only.

Do not introduce yet:
- RoleAssignment
- PermissionResolver
- BuiltInRoles
- grant stores
- policy engine
- secured service decorators
- persistence
- REST
- JWT
- SAML
- OIDC
- Spring Security
- servlet APIs
- user management

Design intent:
A PermissionGrant represents a permission made available at a ResourceScope.

It should not evaluate authorization.
It should not contain an Actor.
It should not contain a Role.
It should not contain grant persistence metadata.
It should not implement inheritance or resolution logic.

Suggested model:
record PermissionGrant(PermissionKey permission, ResourceScope scope)

Validation:
- Reject null PermissionKey.
- Reject null ResourceScope.

Factories:
- PermissionGrant.of(PermissionKey permission, ResourceScope scope)

Tests:
- Creates grant with permission and scope.
- Rejects null permission.
- Rejects null scope.
- Preserves permission.
- Preserves scope.
- Equality works by value.
- Can grant Permissions.CONTENT_ITEM_UPDATE on ContentTypeScope.
- Can grant Permissions.CONTENT_ITEM_PUBLISH on ContentItemScope.
- Does not contain Actor field.
- Does not contain Role field.
- Does not evaluate access.

Important:
Do not implement implication semantics yet.
For example, do not implement contentItem.update implies contentItem.read.
That belongs later in PermissionResolver or permission services.

Do not add site-boundary lookup logic yet.
That belongs later in PermissionResolver.

Expected result:
- New PermissionGrant type.
- Unit tests.
- Full reactor build green.
- Short report with files changed, tests added, and design decisions.