Task H — Introduce BuiltInRoles

Context:
Custos Phase 1 now has:
- Permissions catalog
- RoleKey
- Role
- PermissionGrant
- RoleAssignment

RoleAssignment is intentionally Actor + RoleKey + ResourceScope. It does not embed Role or PermissionKey.

Next, introduce the canonical built-in role blueprints.

Scope:
Introduce BuiltInRoles only.

Do not introduce yet:
- PermissionResolver
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
- Olorin plan execution
- step-up approval

Design intent:
BuiltInRoles is a catalog of canonical Role blueprints.

A Role is a blueprint:
- RoleKey
- Set<PermissionKey>

A built-in role is not an assignment.
A built-in role has no actor.
A built-in role has no scope.
A built-in role does not evaluate access.

Suggested class:
codex.custos.api.model.BuiltInRoles

Suggested role keys:
- SUPER_ADMIN
- SITE_ADMIN
- EDITOR
- COPYWRITER
- REVIEWER
- VIEWER

Suggested role permissions:

SUPER_ADMIN:
- Document as special. It may be represented as a Role with broad permissions, but do not implement bypass logic here.
- Bypass semantics belong to resolver/evaluator/policy layer.

SITE_ADMIN:
- site.read
- site.start
- site.suspend
- site.archive
- contentType.read
- contentType.create
- contentType.update
- contentType.delete
- contentItem.read
- contentItem.create
- contentItem.update
- contentItem.publish
- contentItem.unpublish
- contentItem.archive
- permission.read
- role.assign
- role.revoke

EDITOR:
- contentItem.read
- contentItem.create
- contentItem.update
- contentItem.publish
- contentItem.unpublish
- contentItem.archive

COPYWRITER:
- contentItem.read
- contentItem.create
- contentItem.update

REVIEWER:
- contentItem.read
- contentItem.publish
- contentItem.unpublish

VIEWER:
- contentItem.read

Requirements:
- BuiltInRoles should be a non-instantiable catalog if implemented as a final utility class.
- Each role constant must be a Role.
- Each role must use RoleKey.
- Each role must use Permissions constants.
- Do not implement implication logic.
- Do not implement SUPER_ADMIN bypass logic.
- Do not assign scopes here.
- Do not assign actors here.
- Add tests verifying canonical role keys and permission sets.
- Add tests verifying built-in roles have no scope and no actor.
- Build must pass.

Expected report:
- Files changed.
- Tests added.
- Final role list.
- Any design decisions, especially around SUPER_ADMIN.