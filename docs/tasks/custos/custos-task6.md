Task G — Introduce RoleAssignment

Context:
Custos Phase 1 now has:
- Permissions catalog
- RoleKey
- Role
- PermissionGrant

PermissionGrant is intentionally PermissionKey + ResourceScope only. It does not carry an actor or role.

Now introduce RoleAssignment as the Phase 1 data object that connects an Actor to a RoleKey at a ResourceScope.

Scope:
Introduce RoleAssignment only.

Do not introduce yet:
- PermissionResolver
- BuiltInRoles
- grant stores
- actor direct permission assignment
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
RoleAssignment represents:
Actor has RoleKey at ResourceScope.

It should not:
- embed the full Role blueprint
- contain PermissionKey
- evaluate permissions
- resolve inherited permissions
- apply implication semantics
- enforce hard agent restrictions
- perform site-boundary lookup
- contain persistence metadata

Suggested model:
record RoleAssignment(Actor actor, RoleKey role, ResourceScope scope)

Validation:
- Reject null Actor.
- Reject null RoleKey.
- Reject null ResourceScope.

Factory:
- RoleAssignment.of(Actor actor, RoleKey role, ResourceScope scope)

Tests:
- Creates assignment with actor, role key, and scope.
- Rejects null actor.
- Rejects null role key.
- Rejects null scope.
- Preserves actor.
- Preserves role key.
- Preserves scope.
- Equality works by value.
- Can assign RoleKey.of("COPYWRITER") on SiteScope.
- Can assign RoleKey.of("REVIEWER") on ContentTypeScope.
- Does not contain PermissionKey field.
- Does not contain Role field.
- Does not evaluate access.

Important:
Do not add built-in role constants yet.
Do not enforce AGENT/SUPER_ADMIN restrictions yet.
Do not implement permission resolution yet.
Those belong in later tasks.

Expected result:
- New RoleAssignment type.
- Unit tests.
- Full reactor build green.
- Short report with files changed, tests added, and design decisions.