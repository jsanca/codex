Task C — Harden Custos Phase 0 primitives

Context:
Custos Phase 0 already exists and the build is green. Before moving into roles, grants, permission resolution, or secured service decorators, we want to harden the primitives so Phase 1 is built on stable foundations.

Scope:
Only harden the existing Custos Phase 0 primitives. Do not introduce Role, RoleKey, RoleAssignment, PermissionGrant, PermissionResolver, BuiltInRoles, permission services, secured decorators, persistence, REST, JWT, SAML, OIDC, Spring Security, servlet APIs, or user management.

ADR alignment:
Follow ADR-009. Custos remains a domain authorization kernel. It evaluates Actor + Permission + Resource + Context -> AccessDecision. It must stay transport-agnostic.

Work items:

1. PermissionKey validation
- Ensure PermissionKey rejects null.
- Ensure PermissionKey rejects empty or blank values.
- Preserve the canonical string value exactly when valid.
- Add tests for valid, null, empty, and blank permission keys.

2. ResourceRef invariants
- Ensure GlobalResourceRef is valid as a singleton or value object according to the current style.
- Ensure SiteResourceRef requires a non-null SiteKey.
- Ensure ContentTypeResourceRef requires non-null SiteKey and ContentTypeKey.
- Ensure ContentItemResourceRef requires non-null SiteKey, ContentTypeKey, and ContentItemKey.
- Add tests for each invalid constructor argument.

3. ResourceScope invariants
- Ensure GlobalScope is valid as a singleton or value object according to the current style.
- Ensure SiteScope requires a non-null SiteKey.
- Ensure ContentTypeScope requires non-null SiteKey and ContentTypeKey.
- Ensure ContentItemScope requires non-null SiteKey, ContentTypeKey, and ContentItemKey.
- Add tests for each invalid constructor argument.

4. AccessDecision behavior
- Verify granted decisions expose granted=true.
- Verify denied decisions expose granted=false.
- Verify granted.requireGranted() does not throw.
- Verify denied.requireGranted() throws AccessDeniedException.
- Verify AccessDeniedException keeps the original AccessDecision instance, preferably assertSame.
- Ensure denied decisions preserve a useful reason.

5. SecurityEvaluationContext
- Ensure null/empty handling is intentional.
- If it has factory methods, test default/empty context creation.
- Do not add web/session/JWT-specific context fields.

6. PermissionEvaluator / AccessDecisionService
- Verify public API names are consistent with ADR-009.
- Do not add role/grant resolution yet.
- Do not add persistence.
- Do not add transport concerns.

7. Module hygiene
- Confirm codex-custos does not depend on REST, Spring, servlet APIs, JWT, SAML, OIDC, or persistence modules.
- Confirm module-info.java uses requires transitive only if the public API exposes types from the depended-on module.

Expected result:
- Focused implementation hardening only.
- New or improved unit tests.
- Build success.
- No new architectural concepts beyond Phase 0.
- Short post-task report listing changed files, tests added, and any design decisions.

Important:
If you find a missing invariant that requires a design decision, stop and report it instead of inventing a new model.