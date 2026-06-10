Task E — Introduce RoleKey and Role

Context:
Custos Phase 0 primitives are implemented and hardened. The canonical Permissions catalog exists. We are now starting Custos Phase 1, but only with the role identity/model primitives.

Scope:
Introduce RoleKey and Role only.

Do not introduce yet:
- RoleAssignment
- PermissionGrant
- PermissionResolver
- BuiltInRoles
- scoped grant stores
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
A Role is a permission blueprint, not an authorization decision and not an actor assignment.

ADR-009 examples include:
- SUPER_ADMIN
- SITE_ADMIN
- EDITOR
- COPYWRITER
- REVIEWER
- VIEWER

However, this task should not implement BuiltInRoles yet. It should only create the reusable Role model that future built-in roles may use.

Suggested API:

- RoleKey
    - Value object wrapping the canonical role name.
    - Reject null, empty, and blank values.
    - Preserve the canonical string value exactly when valid.
    - Provide factory method, likely RoleKey.of(String value).
    - Keep naming style consistent with PermissionKey.

- Role
    - Represents a permission blueprint.
    - Contains RoleKey.
    - Contains a set of PermissionKey values.
    - Reject null RoleKey.
    - Reject null permission collection.
    - Reject null PermissionKey elements.
    - Defensively copy permission collection.
    - Expose permissions as an immutable Set.
    - Decide whether empty permission sets are allowed. Recommendation: allow empty roles for now, because custom roles may be assembled gradually, but document the choice.
    - Do not include scope. Scope belongs to RoleAssignment, not Role.
    - Do not include actor. Actor belongs to RoleAssignment, not Role.
    - Do not include grant metadata. That belongs to PermissionGrant / RoleAssignment later.

Suggested factories:
- Role.of(RoleKey key, Set<PermissionKey> permissions)
- Optional convenience overload using varargs may be okay, but avoid ambiguity.

Tests:
- RoleKey accepts valid value.
- RoleKey rejects null.
- RoleKey rejects empty.
- RoleKey rejects blank.
- RoleKey preserves canonical value.
- RoleKey equality/toString behavior consistent with PermissionKey.

- Role rejects null key.
- Role rejects null permission set.
- Role rejects null permission element.
- Role defensively copies permissions.
- Role exposes immutable permissions.
- Role has no scope or actor.
- Role can contain Permissions.CONTENT_ITEM_READ etc.
- If empty roles are allowed, add a test documenting that choice.

Module/API:
- Keep everything in codex-custos API model package unless current package style suggests otherwise.
- No internal implementation yet.
- No new dependencies.

Expected result:
- New RoleKey and Role types.
- Unit tests.
- Full reactor build green.
- Short report with files changed, tests added, and design decisions.