Task 15B — Implement SecuredContentTypeService

Context:
SecuredContentItemService is implemented and reviewed. The service-level authorization matrix test confirms the full internal Custos chain works.

Next:
Implement the same secured decorator pattern for content type operations.

Goal:
Create SecuredContentTypeService as a decorator over ContentTypeService.

Scope:
Implement only SecuredContentTypeService and tests.

Do not implement:

* SecuredSiteService
* runtime Concilium wiring
* REST
* persistence
* Archivum
* workflow
* Olorin
* step-up approval
* restore/purge/delete semantics

Placement:
Use the same module-placement rule as SecuredContentItemService:

* codex-custos internal service
* do not make codex-codex depend on codex-custos
* keep the decorator internal unless an existing composition need says otherwise

Use:

* ContentTypeService as delegate
* ContentTypePermissionsService for authorization
* Supplier<PermissionResolutionSnapshot> for fresh snapshot per operation

Expected mappings:
Use the existing ContentTypeService API as source of truth.

Likely mappings:

* create -> canCreateContentType(...)
* read/find -> canReadContentType(...)
* update -> canUpdateContentType(...)
* archive -> canArchiveContentType(...)

Important vocabulary:

* Use archive vocabulary.
* Do not use delete vocabulary.
* Do not introduce canDeleteContentType.
* Do not introduce CONTENT_TYPE_DELETE.

Rules:

* authorization happens before delegate call
* denied decision throws AccessDeniedException via requireGranted()
* invariant exceptions propagate uncaught
* granted decision delegates exactly once
* delegate return value is preserved
* no resolver logic
* no role lookup
* no scope hierarchy logic
* no permission implication logic
* no REST/security-framework concepts
* no persistence concerns

If ContentTypeService contains methods without matching permission vocabulary:

* Do not pass through sensitive operations silently.
* Fail closed for sensitive unsupported operations.
* For collection reads, pass-through is acceptable only if explicitly documented as pending filtering strategy.

Tests:
Add focused tests for SecuredContentTypeService.

Required tests:

1. create checks canCreateContentType before delegate.
2. read/find checks canReadContentType before delegate.
3. update checks canUpdateContentType before delegate.
4. archive checks canArchiveContentType before delegate.
5. denied create/update/archive does not call delegate.
6. denied decision throws AccessDeniedException.
7. granted decision delegates exactly once.
8. delegate return value is preserved.
9. CustosAgentSuperAdminInvariantViolationException propagates.
10. null guards for constructor dependencies.
11. null guards for method arguments.
12. no delete vocabulary appears.

Run:

* mvn test -pl codex-custos

Expected output:

* files added/changed
* module placement decision
* mappings implemented
* unsupported operations decision, if any
* tests added
* final test count
* any uncertain points
