Task I.1 — Refactor PermissionResolver into request/snapshot/result and small internal strategies

Context:
PermissionResolver currently works and has strong test coverage, but its API is too helper-like:
boolean hasPermission(Actor, PermissionKey, ResourceScope, Collection<RoleAssignment>, Map<RoleKey, Role>)

We want a semantic Custos API:
PermissionResolution resolve(PermissionResolutionRequest request, PermissionResolutionSnapshot snapshot)

Goal:
Refactor without changing behavior.

Scope:
Refactor only. Preserve current behavior and test coverage.
Do not implement AccessDecisionService.
Do not implement direct actor PermissionGrant support.
Do not add persistence, REST, JWT, SAML, OIDC, Spring Security, servlet APIs, or user management.

Required API changes:
1. Introduce PermissionResolutionRequest
- Actor actor
- PermissionKey permission
- ResourceScope target

2. Introduce PermissionResolutionSnapshot
- Collection<RoleAssignment> assignments
- Map<RoleKey, Role> roleRegistry
- Defensive copy where appropriate.
- Reject null collections/maps and null elements/entries if practical.

3. Introduce PermissionResolution
- Explainable result instead of boolean.
- Use sealed interface or equivalent project style.
- Must represent granted/denied.
- Must carry actor, permission, target, and reason.
- Keep it minimal. No full trace yet.

4. Update PermissionResolver
- Replace hasPermission(...) with:
  PermissionResolution resolve(PermissionResolutionRequest request,
  PermissionResolutionSnapshot snapshot)

5. Add Custos exception hierarchy
   Suggested:
- CustosSecurityException
- CustosInvariantViolationException
- CustosAgentSuperAdminInvariantViolationException

Use CustosAgentSuperAdminInvariantViolationException instead of IllegalStateException when an AGENT actor holds SUPER_ADMIN.

Important:
This invariant violation is not a normal authorization denial. It indicates corrupted/invalid security state.

6. Extract scope hierarchy strategy
   Introduce internal:
- ResourceScopeHierarchy
- DefaultResourceScopeHierarchy

Responsibilities:
- parentOf(ResourceScope)
- covers(ResourceScope assignmentScope, ResourceScope targetScope)

Keep internal unless needed by public API.

7. Extract permission implication strategy
   Introduce internal:
- PermissionImplicationRules or DefaultPermissionImplicationRules

Responsibilities:
- direct permission match
- contentItem.update implies contentItem.read
- contentItem.publish implies contentItem.read
- contentItem.publish does not imply contentItem.update
- no other implications

8. Preserve evaluation order
- Hard invariants first.
- SUPER_ADMIN bypass second.
- Normal scoped resolution third.

9. Tests
   Update existing tests from boolean assertions to PermissionResolution assertions.
   Add tests for:
- request null guards
- snapshot null guards
- granted result reason
- denied result reason
- CustosAgentSuperAdminInvariantViolationException
- scope hierarchy strategy if package-private tests are acceptable
- permission implication strategy if package-private tests are acceptable

Expected result:
- PermissionResolver API is semantic.
- DefaultPermissionResolver is easier to read.
- No behavior regression.
- Full reactor build green.
- Short report with files changed, tests changed, and preserved behavior.