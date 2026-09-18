Task H.1 — Add canonical RoleKey constants to BuiltInRoles

Context:
BuiltInRoles already defines the six canonical Role blueprints:
SUPER_ADMIN, SITE_ADMIN, EDITOR, COPYWRITER, REVIEWER, VIEWER.

Before implementing PermissionResolver, expose canonical RoleKey constants so resolver/policy code can refer to role identities without raw strings or repeated BuiltInRoles.X.key() calls.

Scope:
Modify BuiltInRoles only, plus tests.

Add:
- SUPER_ADMIN_KEY
- SITE_ADMIN_KEY
- EDITOR_KEY
- COPYWRITER_KEY
- REVIEWER_KEY
- VIEWER_KEY

Requirements:
- Each key must be a RoleKey.
- Each Role must use its corresponding RoleKey constant.
- Existing role values and permission sets must not change.
- Do not implement PermissionResolver.
- Do not implement bypass logic.
- Do not implement agent restrictions.
- Do not add policy engine, persistence, REST, JWT, SAML, OIDC, Spring Security, servlet APIs, or user management.

Tests:
- Verify each *_KEY has the expected canonical value.
- Verify each built-in Role uses the matching *_KEY.
- Verify SUPER_ADMIN still has all Permissions catalog permissions.
- Verify no bypass/isAdmin/evaluate logic appears in BuiltInRoles.

Expected result:
- Small refactor.
- Tests still green.
- Short report.