# Implementation Report — CODEX-016: Secured Runtime Composition

**Task:** `docs/agents/tasks/runtime/Task-CODEX-016—SecuredRuntimeComposition.md`
**Date:** 2026-08-06
**Implementer:** Clio
**Status:** COMPLETE

---

## Summary

Phase 3.1 Secured Runtime Composition is implemented using approved Option B. Concilium now
exposes an explicit secured runtime path (`ConciliumRuntime.secured(...)`) that enforces Custos
domain authorization on all service operations. The unsecured path (`ConciliumRuntime.inMemory()`)
is preserved as a backward-compatible path for core tests and non-authorization contexts.

`codex-codex` remains pure — it does not depend on `codex-custos`.

---

## Files Changed

### New files

| File | Description |
|------|-------------|
| `codex-custos/src/main/java/codex/custos/api/service/SecuredServiceComposer.java` | Public factory/bridge for building the Custos authorization stack and wrapping services |
| `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeSecuredTest.java` | 13 behavioral tests for `ConciliumRuntime.secured(...)` |

### Modified files

| File | Change |
|------|--------|
| `codex-concilium/pom.xml` | Added `codex-custos` dependency |
| `codex-concilium/src/main/java/module-info.java` | Added `requires codex.custos` |
| `codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java` | Added `RuntimeSecurityMode` enum, `securityMode()` accessor, `siteService()` / `contentTypeService()` / `contentItemService()` accessors, `secured(Supplier<PermissionResolutionSnapshot>)` factory |

---

## Production Changes

### `SecuredServiceComposer` (codex-custos, exported)

Located in `codex.custos.api.service`. Public factory with zero authorization logic.

Responsibilities:
- Creates the default Custos authorization stack (resolver + decision service + permission services)
- Wraps raw Codex services with existing internal `Secured*Service` decorators
- Is the sole bridge between `codex-concilium` and Custos internal implementations

Public API:
```java
// Auth stack factories
SecuredServiceComposer.defaultAccessDecisionService()
SecuredServiceComposer.defaultSitePermissionsService(decisionService)
SecuredServiceComposer.defaultContentTypePermissionsService(decisionService)
SecuredServiceComposer.defaultContentItemPermissionsService(decisionService)

// Service wrapper factories
SecuredServiceComposer.wrapSiteService(delegate, permissionsService, snapshotProvider)
SecuredServiceComposer.wrapContentTypeService(delegate, permissionsService, snapshotProvider)
SecuredServiceComposer.wrapContentItemService(delegate, permissionsService, snapshotProvider)
```

### `ConciliumRuntime` changes

**New inner enum:**
```java
public enum RuntimeSecurityMode { SECURED, UNSECURED }
```

**New factory:**
```java
public static ConciliumRuntime secured(Supplier<PermissionResolutionSnapshot> snapshotProvider)
```
Creates the full in-memory pipeline identical to `inMemory()`, then wraps all three services via
`SecuredServiceComposer`. The `snapshotProvider` is called per operation, not at construction time.

**New service accessors:**
```java
public SiteService siteService()
public ContentTypeService contentTypeService()
public ContentItemService contentItemService()
```
For secured runtimes these return authorization-enforcing decorators. For unsecured runtimes
(`inMemory()`, `compose()`) these return the raw services from the core runtime.

**New metadata accessor:**
```java
public RuntimeSecurityMode securityMode()
```
Diagnostic metadata only. Returns `SECURED` or `UNSECURED` based on the factory path used.

### Runtime composition structure after CODEX-016

```
ConciliumRuntime.secured(snapshotProvider)
  │
  ├─► CodexRuntime.inMemory(forwardingDispatcher, Observance.noop())
  │     └─► assemble()
  │           ├─► siteService = Timed → EventPublishing → CodexSiteService
  │           ├─► contentTypeService = Timed → EventPublishing → CodexContentTypeService
  │           └─► contentItemService = Timed → EventPublishing → Caching → CodexContentItemService
  │
  ├─► SecuredServiceComposer wraps each service:
  │     ├─► SecuredSiteService        ← wraps raw siteService
  │     ├─► SecuredContentTypeService ← wraps raw contentTypeService
  │     └─► SecuredContentItemService ← wraps raw contentItemService
  │
  ├─► IndexRuntime.inMemory(...)
  ├─► ChroniconRuntime.inMemory()
  └─► LocalCodexEventDispatcher (all subscribers)
```

Events still flow: secured decorator authorizes → delegates to raw service → raw service emits
events → dispatcher routes to Index + Chronicon subscribers.

### Dependency graph after CODEX-016

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

`codex-codex` does not depend on `codex-custos`. ✓

---

## Runtime Metadata

`RuntimeSecurityMode` is an enum on `ConciliumRuntime` indicating whether authorization is active.

| Factory | `securityMode()` |
|---------|-----------------|
| `ConciliumRuntime.secured(...)` | `SECURED` |
| `ConciliumRuntime.inMemory()` | `UNSECURED` |
| `ConciliumRuntime.inMemory(Observance)` | `UNSECURED` |
| `ConciliumRuntime.compose(...)` | `UNSECURED` |

This is diagnostic evidence, not the security mechanism. Security is enforced by the service graph —
callers should use `runtime.siteService()` (not `runtime.coreRuntime().siteService()`) on a secured
runtime to get the authorization-enforcing decorator.

---

## Tests Added / Changed

### `ConciliumRuntimeSecuredTest` (13 tests)

| Group | Tests |
|-------|-------|
| `Factory` | `securedRuntimeIsNotNull`, `rejectsNullSnapshotProvider`, `securedRuntimeReportsSecuredMode` |
| `AuthorizationDeny` | `unauthorizedActorCannotCreateSite`, `deniedSiteCreateDoesNotMutateSiteState`, `deniedOperationDoesNotEmitDomainEvents` |
| `AuthorizationGrant` | `superAdminCanCreateSiteTypeAndItem`, `authorizedPublishFlowReachesIndexAndChronicon` |
| `SnapshotProvider` | `snapshotProviderIsCalledPerOperation` |
| `HardInvariants` | `agentWithSuperAdminThrowsInvariantViolation` |
| `Lifecycle` | `inMemoryRuntimeStillWorksAsUnsecuredPath`, `closeIsIdempotentOnSecuredRuntime`, `inMemoryRuntimeReportsUnsecuredMode` |

---

## Validation Results

```bash
git diff --check -- "*.java" "*.xml"
# EXIT: 0 — no whitespace errors in code files
# (trailing whitespace exists only in pre-existing task markdown files)

mvn test -pl codex-custos,codex-concilium -am --no-transfer-progress
# codex-custos:    356 tests — BUILD SUCCESS
# codex-concilium:  40 tests — BUILD SUCCESS
#   ConciliumRuntimeTest:        26 tests
#   ConciliumRuntimeEndToEndTest:  1 test
#   ConciliumRuntimeSecuredTest:  13 tests
```

---

## Secured vs Unsecured Behavior Summary

| Behavior | `inMemory()` | `secured(provider)` |
|----------|-------------|---------------------|
| Authorization on `siteService()` | None | Custos `SecuredSiteService` |
| Authorization on `contentTypeService()` | None | Custos `SecuredContentTypeService` |
| Authorization on `contentItemService()` | None | Custos `SecuredContentItemService` |
| Denied create mutates state | N/A | No — delegate never called |
| Denied create emits events | N/A | No — delegate never called |
| Invariant propagation | N/A | `CustosAgentSuperAdminInvariantViolationException` propagates |
| `securityMode()` | `UNSECURED` | `SECURED` |
| Event pipeline (Index + Chronicon) | Active | Active (authorized ops only) |
| `coreRuntime().siteService()` | Raw service | Raw service (bypass) |

---

## Deviations from CODEX-016

None. All acceptance criteria are satisfied.

---

## Accepted Gaps (Carried Forward)

These were accepted at Phase 1.4 and remain deferred:

- Collection reads (`findAll`, `findByContentType`, `findBySiteKey`) pass through without authorization
- `findByAlias` cannot check `canReadSite` without alias→`SiteKey` resolution
- `delete`, `restore`, `unarchive` are fail-closed (`UnsupportedOperationException`) pending permission vocabulary
- Direct actor `PermissionGrant` support is pending
- Explanation trace is pending
- Denied-operation cache/index consistency tests are pending
- Audit/authorization integration is pending

---

## Follow-Ups

| Priority | Item |
|----------|------|
| Near-future | Collection read authorization — post-retrieval or query-level filtering |
| Near-future | Alias-to-`SiteKey` resolution before `canReadSite` |
| Near-future | Restore / delete / unarchive permission vocabulary |
| Future | Explanation trace on `AccessDecision` |
| Future | Audit integration with denied operations |
