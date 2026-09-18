Task 16 — Phase 3.1 Secured Runtime Composition

Status: Planned
Owner: Clio
Role: Implementation
Target: 20–30 minutes Hard Stop: 45 minutes

Execution Requirements
Apply .claude/skills/process/osk-execution-timebox/SKILL.md
Apply .claude/skills/process/osk-engineering-reporting/SKILL.md

Context:
Deep completed Phase 3.1 secured runtime composition discovery.

Approved approach:
Option B — add a public Custos composer/factory while keeping Secured*Service implementations internal.

Goal:
Add a secured runtime composition path so Concilium can expose secured Codex services without making codex-codex depend on codex-custos.

Scope:
Production code and tests.

Do not modify:

* codex-codex service assembly internals unless absolutely necessary
* SecuredSiteService, SecuredContentTypeService, SecuredContentItemService behavior
* authorization semantics
* permission vocabulary
* persistence
* REST
* Archivum
* Olorin
* workflow

Required production changes:

1. In codex-custos, add a public factory/composer in exported API package:

Suggested class:

* codex.custos.api.service.SecuredServiceComposer

Responsibilities:

* expose factory methods that wrap raw Codex services with secured decorators
* instantiate existing internal Secured*Service classes
* contain zero authorization logic of its own

Suggested methods:

* SiteService wrapSiteService(
  SiteService delegate,
  SitePermissionsService permissionsService,
  Supplier<PermissionResolutionSnapshot> snapshotProvider
  )

* ContentTypeService wrapContentTypeService(
  ContentTypeService delegate,
  ContentTypePermissionsService permissionsService,
  Supplier<PermissionResolutionSnapshot> snapshotProvider
  )

* ContentItemService wrapContentItemService(
  ContentItemService delegate,
  ContentItemPermissionsService permissionsService,
  Supplier<PermissionResolutionSnapshot> snapshotProvider
  )

Rules:

* Validate all arguments with Objects.requireNonNull.
* Return the service interface type, not the concrete secured class.
* Do not export codex.custos.internal.service.
* Keep Secured*Service classes internal.

2. In codex-concilium:

* Add dependency on codex-custos in pom.xml.
* Add requires codex.custos in module-info.java.

3. Add secured runtime factory to ConciliumRuntime:

Suggested method:

* public static ConciliumRuntime secured(Supplier<PermissionResolutionSnapshot> snapshotProvider)

This method should:

* create the current in-memory Codex runtime/service graph
* create Custos default authorization stack:

    * DefaultPermissionResolver
    * DefaultAccessDecisionService
    * DefaultSitePermissionsService
    * DefaultContentTypePermissionsService
    * DefaultContentItemPermissionsService
* wrap SiteService, ContentTypeService, and ContentItemService using SecuredServiceComposer
* expose the wrapped services through the returned ConciliumRuntime
* preserve current event/index/Chronicon/Observance pipeline

Important:

* snapshotProvider must be called per operation by secured decorators, not eagerly once at runtime creation.
* Keep ConciliumRuntime.inMemory() working as the existing unsecured/back-compat path.
* Do not rename inMemory() in this task.
* Do not make secured() call inMemory() if that prevents substituting wrapped services into the returned runtime.

4. If ConciliumRuntime currently has final fields or constructor shape that makes wrapping awkward:

* make the smallest safe change necessary
* preserve existing tests
* do not introduce a new assembly module

Required tests:

1. ConciliumRuntime.secured(snapshotProvider) returns non-null runtime.
2. Unauthorized actor calling site create through secured runtime throws AccessDeniedException.
3. Authorized SUPER_ADMIN can create site, content type, and content item through secured runtime.
4. Authorized publish flow still reaches Index and Chronicon.
5. Denied site create does not mutate site state.
6. Denied operation does not emit domain events to the composed event pipeline.
7. ConciliumRuntime.inMemory() still works as unsecured/back-compat path.
8. close() is idempotent on secured runtime.
9. snapshotProvider is called per secured operation, not only once at runtime creation.
10. AGENT + SUPER_ADMIN assignment throws CustosAgentSuperAdminInvariantViolationException through secured runtime.

Testing guidance:

* Prefer behavioral assertions over concrete class assertions.
* Do not assert that runtime.siteService() is instance of SecuredSiteService if that requires exposing internal classes.
* Prove security by behavior:

    * unauthorized actor denied
    * delegate-side state unchanged
    * authorized actor succeeds
    * invariant propagates
* Use AssertJ.
* Keep tests readable and grouped.

Run:

* mvn test -pl codex-custos,codex-concilium

Expected output:

* files added/changed
* dependency/module-info changes
* composer API added
* ConciliumRuntime.secured(...) behavior
* tests added
* final test count
* any composition limitations or uncertain points
