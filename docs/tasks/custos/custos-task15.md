Task 15A — Implement SecuredContentItemService

Context:
Custos Phase 1 now has the authorization core loop:

AccessDecisionRequest
-> AccessDecisionService
-> PermissionResolver
-> PermissionResolution
-> AccessDecision

And domain-specific permission services:

* SitePermissionsService
* ContentTypePermissionsService
* ContentItemPermissionsService

The next step is to wire Custos into the Codex domain service layer through explicit secured decorators.

Goal:
Create a secured decorator for content item operations.

Scope:
Implement SecuredContentItemService only.

Do not implement:

* SecuredSiteService
* SecuredContentTypeService
* runtime wiring in Concilium
* REST
* persistence
* Archivum archive store
* workflow/Iter
* Olorin
* step-up approval
* restore/purge/delete semantics

Expected class:

* codex-codex or codex-custos/codex integration package depending on current module boundaries.

Important module-boundary question:
Before writing code, inspect the existing module dependencies.

Preferred rule:

* codex-codex core should not depend on codex-custos if that violates the current architecture.
* If secured decorators belong outside the core module, place them where composition/adapters currently live or report the boundary issue before implementing.

Design intent:
SecuredContentItemService is a decorator over ContentItemService.

It should:

1. Receive:

    * delegate ContentItemService
    * ContentItemPermissionsService
    * PermissionResolutionSnapshot or a provider/source for snapshots, depending on existing patterns
2. Check the proper permission before delegating.
3. Throw AccessDeniedException when AccessDecision is denied.
4. Let Custos invariant exceptions propagate.
5. Never mutate domain state before authorization succeeds.
6. Never publish events before authorization succeeds.
7. Never update cache/index directly.
8. Remain transport-agnostic.

Operations to secure:
Use the existing ContentItemService API as the source of truth.

Expected mappings:

* create -> contentItemPermissionsService.canCreateContentItem(...)
* read/find -> contentItemPermissionsService.canReadContentItem(...)
* update -> contentItemPermissionsService.canUpdateContentItem(...)
* publish -> contentItemPermissionsService.canPublishContentItem(...)
* unpublish -> contentItemPermissionsService.canUnpublishContentItem(...)
* archive -> contentItemPermissionsService.canArchiveContentItem(...)

Do not add:

* canDeleteContentItem
* canRestoreContentItem
* canPurgeContentItem

Denied behavior:
If the AccessDecision is denied:

* throw AccessDeniedException carrying the denied decision if current model supports it
* do not call the delegate
* do not emit domain events
* do not change cache/index/audit indirectly

Granted behavior:
If the AccessDecision is granted:

* call the delegate exactly once
* return the delegate result unchanged

Testing:
Add focused tests for SecuredContentItemService.

Use a recording/stub ContentItemService delegate and a fake ContentItemPermissionsService.

Required tests:

1. create checks canCreateContentItem before delegate.
2. read/find checks canReadContentItem before delegate.
3. update checks canUpdateContentItem before delegate.
4. publish checks canPublishContentItem before delegate.
5. unpublish checks canUnpublishContentItem before delegate.
6. archive checks canArchiveContentItem before delegate.
7. denied create does not call delegate.
8. denied update does not call delegate.
9. denied publish does not call delegate.
10. denied decision throws AccessDeniedException.
11. granted decision delegates exactly once.
12. delegate return value is preserved.
13. CustosAgentSuperAdminInvariantViolationException propagates.
14. null guards for constructor dependencies.
15. null guards for method arguments as appropriate.

Quality rules:

* Keep it boring.
* No resolver logic.
* No role lookup.
* No ResourceScope hierarchy logic.
* No permission implication logic.
* No REST/security-framework concepts.
* Meaningful variable names.
* AssertJ style.
* @Nested and @DisplayName in tests.

Expected output:

* files added/changed
* explanation of module placement decision
* mappings implemented
* tests added
* commands run
* final test counts
* any boundary concerns
