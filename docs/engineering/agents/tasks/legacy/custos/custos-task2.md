Task D — Add canonical Custos permission catalog

Add a small catalog of built-in PermissionKey constants based on ADR-009.

Do not implement roles, grants, resolver, policies, secured services, persistence, REST, JWT, Spring Security, SAML, OIDC, servlet APIs, or user management.

Suggested permissions:
- site.read
- site.create
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
- permission.grant
- permission.revoke
- role.assign
- role.revoke

Expected:
- A final utility/catalog class, probably Permissions or BuiltInPermissions.
- Constants must reuse PermissionKey.
- Tests verifying canonical values.
- No string duplication in tests if avoidable.
- Build success.