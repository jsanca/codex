Task 13 — Define Domain-Specific Permission Services

Context:
Custos now has the core authorization loop:

AccessDecisionRequest
-> DefaultAccessDecisionService
-> PermissionResolver
-> PermissionResolution
-> AccessDecision

Phase 1.1 and 1.2 are complete:

* AccessDecisionService wiring verified.
* AccessDecisionService tests verified and cleaned up.
* codex-custos suite is green.

Next step:
Introduce small domain-specific permission services that translate Codex domain operation intent into Custos authorization requests.

Goal:
Avoid scattering PermissionKey + ResourceRef + ResourceScope construction across future secured decorators.

Scope:
Add API-level permission service interfaces and internal default implementations for the main Codex domains:

* SitePermissionsService
* ContentTypePermissionsService
* ContentItemPermissionsService

These services should be thin, explicit adapters over AccessDecisionService.

They should answer authorization questions for domain operations, such as:

Site:

* canCreateSite
* canReadSite
* canUpdateSite
* canDeleteSite
* canManageSiteSettings, only if already represented by current Permissions catalog

ContentType:

* canCreateContentType
* canReadContentType
* canUpdateContentType
* canDeleteContentType / deprecate, depending on existing lifecycle vocabulary

ContentItem:

* canCreateContentItem
* canReadContentItem
* canUpdateContentItem
* canPublishContentItem
* canUnpublishContentItem
* canArchiveContentItem
* canDeleteContentItem

Important:
Use only permissions that already exist in the Permissions catalog.
Do not invent new PermissionKey constants unless a missing permission is clearly required and documented in the report.

Design intent:
These services should map:

Domain operation intent
-> PermissionKey
-> ResourceRef
-> ResourceScope
-> AccessDecisionService.evaluate(...)

They should not:

* perform permission resolution themselves
* inspect RoleAssignment
* inspect Role
* inspect BuiltInRoles
* walk ResourceScope hierarchy
* apply permission implication rules
* mutate domain state
* dispatch events
* talk to repositories
* know about REST/HTTP
* know about JWT/SAML/OIDC/Spring Security
* know about Olorin or step-up approval

API shape:
Prefer explicit methods that return AccessDecision.

Example style:

AccessDecision canPublishContentItem(
Actor actor,
ContentItemKey itemKey,
ContentTypeKey contentTypeKey,
SiteKey siteKey,
PermissionResolutionSnapshot snapshot
)

or equivalent based on the current domain identity model.

Each method should:

1. Build the correct ResourceRef.
2. Build the correct ResourceScope.
3. Use the correct PermissionKey.
4. Build AccessDecisionRequest.
5. Delegate to AccessDecisionService.

Do not throw AccessDeniedException in these permission services yet unless the existing AccessDecisionService contract already requires it.
For now, returning AccessDecision keeps them usable by future secured decorators.

ResourceRef vs ResourceScope:
Keep the distinction explicit.

Examples:

* Site operation:
  ResourceRef = SiteResourceRef(siteKey)
  ResourceScope = SiteScope(siteKey)

* ContentType operation:
  ResourceRef = ContentTypeResourceRef(siteKey, contentTypeKey)
  ResourceScope = ContentTypeScope(siteKey, contentTypeKey)

* ContentItem operation:
  ResourceRef = ContentItemResourceRef(siteKey, contentTypeKey, contentItemKey)
  ResourceScope = ContentItemScope(siteKey, contentTypeKey, contentItemKey)

If exact constructors/types differ, follow the existing model.

Testing:
Add tests proving each permission service builds the correct request and delegates to AccessDecisionService.

Recommended test strategy:
Use a recording/fake AccessDecisionService that captures:

* actor
* permission
* ResourceRef
* targetScope
* snapshot

Test categories:

1. Site permission service maps operations to correct permissions/resources/scopes.
2. ContentType permission service maps operations to correct permissions/resources/scopes.
3. ContentItem permission service maps operations to correct permissions/resources/scopes.
4. Returned AccessDecision is the decision returned by AccessDecisionService.
5. Null guards for constructor dependencies and method arguments.
6. No role/assignment/resolver logic leaks into these services.

Quality rules:

* Keep classes boring.
* Prefer meaningful names over short variables.
* Use @Nested and @DisplayName in tests.
* Prefer AssertJ.
* Do not over-abstract prematurely.
* Do not introduce generic “DomainPermissionService” unless the duplication becomes painful and the abstraction is obvious.
* Explicit domain-specific services are preferred for readability.

Expected output:

* New API interfaces if appropriate.
* New internal default implementations.
* Tests.
* Full codex-custos test suite green.
* Short report:

    * files added/changed
    * methods introduced
    * permission mappings
    * ResourceRef/ResourceScope mappings
    * tests run and final count
    * any missing permission discovered
    * any uncertain design point

Do not modify:

* secured decorators
* codex-codex services
* REST/Porta
* persistence/Archivum
* Olorin/Imaginarium
* workflow/Iter
