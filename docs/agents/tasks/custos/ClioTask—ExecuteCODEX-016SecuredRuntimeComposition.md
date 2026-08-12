Clio Task — Execute CODEX-016 Secured Runtime Composition

Task file:
docs/agents/tasks/runtime/Task-CODEX-016—SecuredRuntimeComposition.md

Status:
Ready for implementation.

Execution requirements:

* Read the task file before coding.
* Apply OSK execution timebox if available.
* Apply OSK engineering reporting if available.
* No commits.
* Create an implementation report under:

docs/agents/reports/runtime/

* Create or update a checkpoint if the task completes or if the hard stop is reached.

Primary goal:
Implement Phase 3.1 Secured Runtime Composition using approved Option B.
(see DeepReview—Phase3.1SecuredRuntimeCompositionDiscovery—REPORT.md)
Approved architectural decisions:

* Custos exposes a public composer/factory.
* Secured*Service implementations remain internal.
* Concilium depends on Custos.
* codex-codex remains pure.
* ConciliumRuntime.secured(...) exposes secured services.
* ConciliumRuntime.inMemory() remains unsecured/back-compat.
* Runtime metadata should indicate SECURED vs UNSECURED.

Important runtime metadata rule:
Runtime metadata is diagnostic evidence, not the security mechanism.
Security is enforced by the service graph exposed by the runtime.

Expected implementation:

1. Add public SecuredServiceComposer in codex.custos.api.service.
2. Keep SecuredSiteService, SecuredContentTypeService, and SecuredContentItemService internal.
3. Add codex-custos dependency to codex-concilium.
4. Add requires codex.custos to codex-concilium module-info.java.
5. Add ConciliumRuntime.secured(Supplier<PermissionResolutionSnapshot> snapshotProvider).
6. Preserve ConciliumRuntime.inMemory().
7. Add runtime metadata:

    * SECURED
    * UNSECURED
8. Ensure secured runtime exposes secured behavior by behavior, not by leaking concrete internal class assertions.

Required validation:

* mvn test -pl codex-custos,codex-concilium
* git diff --check

Expected report:

* files changed
* production changes
* tests added/changed
* validation commands and results
* runtime metadata shape
* secured/unsecured behavior summary
* any deviations from CODEX-016
* follow-ups
