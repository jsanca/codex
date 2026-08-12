# Review Report — Phase 3.1: Secured Runtime Composition Discovery

**Report type:** review
**Date:** 2026-08-05
**Reviewer:** Deep
**Task:** `docs/tasks/reviewer/DeepReview—Phase3.1SecuredRuntimeCompositionDiscovery.md`

---

## Checkpoint Status

- **OPEN checkpoint:** `docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md` — confirms Phase 1.4 complete; recommends Phase 3.1 secured runtime composition as next work. This review is consistent with that checkpoint.
- **No recovery checkpoint exists** for this review task.

---

## Evidence: Files Inspected

| File | Purpose |
|------|---------|
| `codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java` | Runtime composition entry point |
| `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeTest.java` | Assembly and pipeline tests (22 tests) |
| `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeEndToEndTest.java` | Integration publish-flow test |
| `codex-concilium/pom.xml` | Dependencies (no custos dep) |
| `codex-concilium/src/main/java/module-info.java` | Exports only `api.runtime` |
| `codex-codex/src/main/java/codex/codex/api/runtime/CodexRuntime.java` | Service assembly root (assemble method) |
| `codex-codex/src/main/java/codex/codex/internal/service/EventPublishingSiteService.java` | Event decorator in chain |
| `codex-codex/src/main/java/codex/codex/internal/service/EventPublishingContentTypeService.java` | Event decorator in chain |
| `codex-codex/src/main/java/codex/codex/internal/service/EventPublishingContentItemService.java` | Event decorator in chain |
| `codex-codex/src/main/java/codex/codex/internal/service/ForwardingSiteService.java` | Decorator base (not used by Custos) |
| `codex-codex/src/main/java/codex/codex/internal/service/ForwardingContentTypeService.java` | Decorator base (not used by Custos) |
| `codex-codex/src/main/java/codex/codex/internal/service/ForwardingContentItemService.java` | Decorator base (not used by Custos) |
| `codex-codex/src/main/java/codex/codex/api/model/service/SiteService.java` | Service interface |
| `codex-codex/src/main/java/codex/codex/api/model/service/ContentTypeService.java` | Service interface |
| `codex-codex/src/main/java/codex/codex/api/model/service/ContentItemService.java` | Service interface |
| `codex-codex/src/main/java/module-info.java` | No custos dependency ✓ |
| `codex-codex/pom.xml` | Only depends on fundamentum ✓ |
| `codex-custos/src/main/java/codex/custos/internal/service/SecuredSiteService.java` | Authorization decorator |
| `codex-custos/src/main/java/codex/custos/internal/service/SecuredContentTypeService.java` | Authorization decorator |
| `codex-custos/src/main/java/codex/custos/internal/service/SecuredContentItemService.java` | Authorization decorator |
| `codex-custos/src/main/java/codex/custos/api/service/ContentItemPermissionsService.java` | Permission service interface |
| `codex-custos/src/main/java/codex/custos/api/service/ContentTypePermissionsService.java` | Permission service interface |
| `codex-custos/src/main/java/codex/custos/api/service/SitePermissionsService.java` | Permission service interface |
| `codex-custos/src/main/java/codex/custos/internal/service/DefaultContentItemPermissionsService.java` | Default implementation |
| `codex-custos/src/main/java/codex/custos/internal/service/DefaultContentTypePermissionsService.java` | Default implementation |
| `codex-custos/src/main/java/codex/custos/internal/service/DefaultSitePermissionsService.java` | Default implementation |
| `codex-custos/src/main/java/codex/custos/api/service/AccessDecisionService.java` | Decision service interface |
| `codex-custos/src/main/java/codex/custos/internal/service/DefaultAccessDecisionService.java` | Default implementation |
| `codex-custos/src/main/java/module-info.java` | Exports api.exception, api.model, api.service |
| `codex-custos/pom.xml` | Depends on codex-codex ✓ |
| `docs/CODEX-ROADMAP-PHASES.md` | Phase 3.1 definition |
| `docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md` | Phase 1.4 checkpoint |
| `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md` | Custos implementation status |
| `docs/modules/MODULE-RESPONSIBILITIES.md` | Module boundary definitions |

---

## Validation

### Verification method

Static code review only. No build or test commands were executed.

### Claims verified by inspection

| Claim | Evidence |
|-------|----------|
| `codex-codex` depends only on `codex-fundamentum` | `codex-codex/pom.xml` — no other Codex module dependency |
| `codex-codex` does not know about `codex-custos` | `codex-codex/module-info.java` — no `requires codex.custos` |
| `codex-custos` depends on `codex-codex` | `codex-custos/pom.xml` — dependency present |
| `Secured*Service` classes are `public` but in `internal` package | Source inspection of all three decorator files |
| `module-info.java` does not export internal package | `codex-custos/module-info.java` — no `exports codex.custos.internal` |
| `ConciliumRuntime` does not depend on `codex-custos` | `codex-concilium/pom.xml` — no custos dependency |
| Service assembly is single-point | `CodexRuntime.assemble()` lines 216-237 — all three services created inline |
| Custos does not use `Forwarding*Service` | Source inspection of `Secured*Service` files — direct `implements` without forwarding base |
| Secured decorators use `Supplier<PermissionResolutionSnapshot>` | Constructor signatures of all three `Secured*Service` classes |
| `PermissionResolutionSnapshot` is immutable (defensively copied) | AGENTS.md / ADR-009 confirmed; snapshot source inspection |

---

## Findings

### Current composition map

```
ConciliumRuntime.inMemory()
  │
  ├─► CodexRuntime.inMemory(forwardingDispatcher, observance)
  │     └─► assemble() [lines 216-237]
  │           ├─► siteService = Timed → EventPublishing → CodexSiteService
  │           ├─► contentTypeService = Timed → EventPublishing → CodexContentTypeService
  │           └─► contentItemService = Timed → EventPublishing → Caching → CodexContentItemService
  │
  ├─► IndexRuntime.inMemory(projectionReader, observance)
  ├─► ChroniconRuntime.inMemory()
  └─► LocalCodexEventDispatcher(allSubscribers)
```

### Q&A

**1. Where are raw domain services created?** — `CodexRuntime.assemble()`, lines 216-237. Inline constructor chains, no factories.

**2. Is there a composition root?** — Two levels: service graph in `CodexRuntime.assemble()`, event pipeline in `ConciliumRuntime.inMemory()`/`compose()`.

**3. Does Concilium own service graph assembly?** — No. It owns event subscriber composition. But it's the natural place for security wrapping since it already composes module-level concerns.

**4. Can custos participate without making codex depend on custos?** — Yes. Concilium adds a dep on custos and wraps services post-construction.

**5. Safest wrapping approach?** — Wrap after construction. Do not modify `CodexRuntime.assemble()`.

**6. Should secured runtime be default?** — Yes. Roadmap says so. Unsecured via deliberate escape hatch.

**7. Is unsecured runtime still needed?** — Yes, for core unit tests, custos decorator tests, and integration tests.

**8. How to supply snapshot?** — `Supplier<PermissionResolutionSnapshot>` (already the decorator design).

**9. How to supply roles/assignments?** — A `CustosRuntime` wrapper holding snapshot + supplier.

**10. How to test secured services are exposed?** — Behavioral: unauthorized actor gets `AccessDeniedException`, authorized actor succeeds.

### Option evaluation

**Option A — Concilium wraps directly:** Viable but needs qualified export of internal classes. Minor module hygiene concern.

**Option B — Custos public factory (RECOMMENDED):** Cleanest. Factory in exported package; implementations stay internal. Low code, one new dep on concilium.

**Option C — New assembly module:** Rejected. Over-engineering for 14-module system.

**Option D — Status quo (tests only):** Rejected. All production paths run unsecured. Phase 3.1 calls for composition.

---

## Recommended Integration: Option B

```
ConciliumRuntime.secured(snapshotProvider)
           │
  ┌────────┼────────┐
  ▼        ▼        ▼
wrapSite  wrapCtype wrapCitem   ← public SecuredServiceComposer in codex.custos.api.service
  │        │        │
  ▼        ▼        ▼
Secured*Service (internal)       ← existing decorators (unchanged)
  │        │        │
  ▼        ▼        ▼
CodexRuntime.siteService() etc.  ← raw services from CodexRuntime
```

### What changes

| Module | Change |
|--------|--------|
| `codex-custos` | +1 public class `SecuredServiceComposer` in `codex.custos.api.service` (exported). Three factory methods. |
| `codex-concilium/pom.xml` | +1 dependency on `codex-custos` |
| `codex-concilium/module-info` | +1 `requires codex.custos` |
| `codex-concilium` | +1 `ConciliumRuntime.secured(snapshot)` factory method |
| `codex-codex` | **No changes** |
| `codex-fundamentum` | **No changes** |
| `codex-index`, `codex-chronicon` | **No changes** |
| `Secured*Service` classes | **No changes** (stay internal) |

### Dependency graph after integration

```
codex-concilium
  ├── codex-custos        ★ NEW
  │     ├── codex-codex
  │     └── codex-fundamentum
  ├── codex-codex
  ├── codex-index
  ├── codex-chronicon
  └── codex-fundamentum
```

---

## Rejected Approaches

| Approach | Reason |
|----------|--------|
| Modify `CodexRuntime.assemble()` | Core purity violation — Codex would need Custos types |
| Export `Secured*Service` from internal package | Module hygiene — security implementations as public API |
| New dedicated assembly module | Over-engineering at current scale |
| Combine into `CodexRuntime` directly | Wrong dependency direction (`codex → custos`) |
| Global singleton snapshot | Must support runtime assignment rotation |
| Use `Forwarding*Service` in Custos | Checkpoint rejects — must decide each method explicitly |

---

## Minimal Next Task

```
Task: Phase 3.1 — Secured Runtime Composition

1. codex-custos: Add public SecuredServiceComposer in codex.custos.api.service
   - wrapSiteService(delegate, permissionsService, snapshotProvider)
   - wrapContentTypeService(delegate, permissionsService, snapshotProvider)
   - wrapContentItemService(delegate, permissionsService, snapshotProvider)
   (Reuses existing Secured*Service constructors — zero new authorization logic.)

2. codex-concilium pom.xml: Add dependency on codex-custos
3. codex-concilium module-info: Add requires codex.custos
4. Add ConciliumRuntime.secured(snapshot) factory:
   - Creates CodexRuntime.inMemory(forwardingDispatcher, observance)
   - Creates AccessDecisionService → Default*PermissionsServices
   - Wraps all three services via SecuredServiceComposer
   - Exposes wrapped services
5. Preserve existing ConciliumRuntime.inMemory() for unsecured back-compat
6. Tests: 10 tests listed below

Estimated: ~100 lines new production code + tests.
```

---

## Required Tests

| # | Test | Proves |
|---|------|--------|
| 1 | `ConciliumRuntime.secured(snapshot)` returns non-null | Factory works |
| 2 | Unauthorized actor → `AccessDeniedException` on site create | Authorization enforced |
| 3 | Authorized SUPER_ADMIN creates site/type/item | Authorized path works |
| 4 | Publish flow reaches Index + Chronicon when authorized | Event pipeline preserved |
| 5 | Site count unchanged after denied create | No state mutation on deny |
| 6 | Event recorder empty after denied operation | No events emitted on deny |
| 7 | `ConciliumRuntime.inMemory()` still works | Unsecured back-compat preserved |
| 8 | `close()` idempotent on secured runtime | Shutdown not broken |
| 9 | Snapshot supplier called per-operation | Fresh security context per call |
| 10 | AGENT+SUPER_ADMIN → `CustosAgentSuperAdminInvariantViolationException` | Hard invariants propagate through composition |

---

## Limitations and Unresolved Issues

### Validation skipped

- **No `mvn clean verify` executed** — this is a static review. Build/tests were not run. The reviewer inspected source files and documentation only.
- **No test runner output captured** — test claim "22 tests exist in ConciliumRuntimeTest" is based on source inspection (counting `@Test` annotations), not a test run.

### Accepted gaps (carried forward from Phase 1.4 checkpoint)

- Collection read filtering (`findByContentType`, `findBySiteKey`, `findAll`) remains pass-through
- `Site.findByAlias` authorization requires alias-to-SiteKey resolution
- Content item delete/restore and site unarchive permission semantics are deferred
- Denied-operation cache/index/event consistency tests remain future work
- Audit/authorization integration remains future work

### Unresolved

- Whether `slf4j-simple` should move from `<scope>test</scope>` to compile scope in `codex-concilium` — currently no production logging binding exists, so runtime logs will silently disappear
- Whether `ConciliumRuntime.inMemory()` should eventually be deprecated/renamed to `inMemoryUnsecured()` when `secured()` becomes the default

---

## References

- `docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md` — Phase 1.4 completion checkpoint
- `docs/CODEX-ROADMAP-PHASES.md` — Phase 3.1 definition
- `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md` — Custos status tracker
- `docs/security/ADR-009.md` — Full Custos authorization spec
- `docs/modules/MODULE-RESPONSIBILITIES.md` — Module boundary definitions
- `AGENTS.md` — Project conventions and module map
