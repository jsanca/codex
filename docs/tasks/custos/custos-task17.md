Task 15C — Implement SecuredSiteService

Context:
SecuredContentItemService and SecuredContentTypeService are implemented and tested.

Now implement the secured decorator for site operations.

Goal:
Create SecuredSiteService as a decorator over SiteService.

Scope:
Implement only SecuredSiteService and tests.

Do not implement:

* runtime Concilium wiring
* REST
* persistence
* Archivum
* workflow
* Olorin
* step-up approval
* restore/purge/delete semantics
* direct actor grants

Placement:
Use the same placement rule as the previous secured decorators:

* codex-custos internal service
* codex-codex must remain untouched
* do not make codex-codex depend on codex-custos

Use:

* SiteService as delegate
* SitePermissionsService for authorization
* Supplier<PermissionResolutionSnapshot> for fresh snapshot per operation

Expected mappings:
Use the existing SiteService API as source of truth.

Likely mappings:

* create -> canCreateSite
* findByKey / read -> canReadSite
* start -> canStartSite
* suspend -> canSuspendSite
* archive -> canArchiveSite

Collection reads:

* findAll or equivalent may remain pass-through only if explicitly documented as pending filtering strategy.

Unsupported sensitive operations:

* If SiteService has update/delete/restore/purge-like methods without permission vocabulary, do not pass them through silently.
* Fail closed for sensitive unsupported operations.

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

Tests:
Add focused tests for SecuredSiteService.

Required tests:

1. create checks canCreateSite before delegate.
2. find/read checks canReadSite before delegate.
3. start checks canStartSite before delegate.
4. suspend checks canSuspendSite before delegate.
5. archive checks canArchiveSite before delegate.
6. denied create/start/suspend/archive does not call delegate.
7. denied decision throws AccessDeniedException.
8. granted decision delegates exactly once.
9. delegate return value is preserved where applicable.
10. CustosAgentSuperAdminInvariantViolationException propagates.
11. null guards for constructor dependencies.
12. null guards for method arguments.
13. collection reads, if any, are documented as future filtering.
14. no unsupported sensitive method passes through unauthenticated.

Quality rules:

* Keep the decorator boring.
* Follow the same style as SecuredContentItemService and SecuredContentTypeService.
* Use @Nested and @DisplayName.
* Prefer AssertJ.
* Use meaningful variable names.
* Do not introduce generic secured decorator abstractions yet.

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
