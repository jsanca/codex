# Review Report — CODEX-017: Denied-Operation Side-Effect Consistency

**Report type:** review
**Date:** 2026-08-08
**Reviewer:** Deep
**Task:** `docs/agents/tasks/custos/CODEX-017—Denied-operationSide-effectConsistency.md`
**Implementation report:** `docs/agents/reports/runtime/CODEX-017-DeniedSideEffectConsistency-REPORT.md`
**Checkpoint:** `docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-RUNTIME-COMPOSITION.md` (CODEX-016 completed)

---

## PASS / WARN / BLOCKER Summary

| Verdict | Count | Items |
|---------|-------|-------|
| **PASS** | 10 | Coverage across 4 denied-operation scenarios, all 6 assertion dimensions, top-level accessor usage, baseline capture pattern, coreRuntime limited to inspection, no production changes, tests pass, existing tests intact, accepted gaps not expanded |
| **WARN** | 1 | Index side-effect assertion is transitively inferred (event-driven) rather than directly observed — architecturally valid but less direct than a stub read would be |
| **BLOCKER** | 0 | — |

**Verdict:** Accept CODEX-017. No changes required.

---

## Validation

### Commands executed and results

```bash
$ git diff --check -- "*.java" "*.xml"
# EXIT: 0 — no whitespace errors

$ git status --short -- "codex-concilium/**"
# ?? codex-concilium/.../ConciliumRuntimeDeniedSideEffectsTest.java  ← new (CODEX-017)
# ?? codex-concilium/.../ConciliumRuntimeSecuredTest.java             ← new (CODEX-016)
# ?? codex-concilium/.../RuntimeSecurityMode.java                     ← new (CODEX-016)
# M  codex-concilium/pom.xml, ConciliumRuntime.java, module-info.java ← CODEX-016
# No production code changed by CODEX-017

$ mvn test -pl codex-concilium -am --no-transfer-progress
# 57 tests, 0 failures — BUILD SUCCESS
#   ConciliumRuntimeTest:                 26 tests
#   ConciliumRuntimeEndToEndTest:          1 test
#   ConciliumRuntimeSecuredTest:          13 tests
#   ConciliumRuntimeDeniedSideEffectsTest: 17 tests  ← NEW
# Total time: ~11s
```

### Evidence: files inspected

| File | Purpose |
|------|---------|
| `codex-concilium/.../ConciliumRuntimeDeniedSideEffectsTest.java` | New test class (353 lines, 17 tests) |
| `codex-concilium/.../ConciliumRuntimeSecuredTest.java` | Existing secured tests (287 lines, 13 tests) |
| `codex-concilium/.../ConciliumRuntime.java` | Runtime composition (verified event pipeline for index inference) |
| `docs/agents/tasks/custos/CODEX-017—Denied-operationSide-effectConsistency.md` | Task definition |
| `docs/agents/reports/runtime/CODEX-017-DeniedSideEffectConsistency-REPORT.md` | Implementation report |

---

## 1. Behavior Coverage Assessment

### Denied-site-create (4 tests)

| Test | What it proves | Verifies dimension |
|------|---------------|--------------------|
| `throwsAccessDeniedException` | `AccessDeniedException` thrown | Authorization gate |
| `noEventsRecorded` | `recordedEvents()` empty | No domain events |
| `noChroniconAuditRecord` | `findAll()` empty | No Chronicon audit |
| `siteStateRemainsEmpty` | `findAll()` empty | No domain state mutation |

**PASS.** All four side-effect dimensions covered:
- Authorization (exception type) ✓
- Domain state (no site created) ✓
- Domain events (none emitted) ✓
- Chronicon audit (none recorded) ✓

Index not checked here because there's no index operation for site create — correct, index only tracks published content items.

### Denied-content-type-create (4 tests)

| Test | What it proves | Verifies dimension |
|------|---------------|--------------------|
| `throwsAccessDeniedException` | `AccessDeniedException` thrown | Authorization gate |
| `noNewEventsAfterDenied` | Event count == baseline | No domain events |
| `noChroniconRecordAfterDenied` | Audit count == baseline | No Chronicon audit |
| `siteHasNoContentTypesAfterDenied` | `findBySiteKey` empty | No domain state mutation |

**PASS.** Uses baseline-capture pattern (event/audit counts captured after authorized site creation, before denied operation). This isolates the denied operation's effects from setup effects. ✓

### Denied-content-item-publish (5 tests)

| Test | What it proves | Verifies dimension |
|------|---------------|--------------------|
| `throwsAccessDeniedException` | `AccessDeniedException` thrown | Authorization gate |
| `itemRemainsInDraft` | `status == DRAFT`, `publishedRevisionId == null` | No domain state mutation |
| `noNewEventsAfterDenied` | Event count == baseline | No domain events |
| `noChroniconRecordAfterDenied` | Audit count == baseline | No Chronicon audit |
| `indexNotUpdatedAfterDeniedPublish` | Event count == baseline → no subscriber invocation | No index update |

**PASS.** Full setup pipeline (site → content type with TITLE field → activate → draft item) before denial. Baseline captures all setup events and audit records. Item-level state check asserts both DRAFT status and null published revision. ✓

### Denied-content-item-update (4 tests)

| Test | What it proves | Verifies dimension |
|------|---------------|--------------------|
| `throwsAccessDeniedException` | `AccessDeniedException` thrown | Authorization gate |
| `itemWorkingRevisionUnchanged` | `currentWorkingRevisionId()` equals original | No domain state mutation |
| `noNewEventsAfterDenied` | Event count == baseline | No domain events |
| `noChroniconRecordAfterDenied` | Audit count == baseline | No Chronicon audit |

**PASS.** Captures `originalItem` at setup time, then asserts working revision ID didn't change after the denied update attempt. ✓

---

## 2. Side-Effect Coverage Assessment

### Coverage matrix

| Service | Operation | AccessDeniedException | Domain state | Domain events | Chronicon audit | Index |
|---------|-----------|-----------------------|--------------|---------------|-----------------|-------|
| `siteService()` | `create` | ✓ | ✓ | ✓ | ✓ | N/A |
| `contentTypeService()` | `create` | ✓ | ✓ | ✓ (baseline) | ✓ (baseline) | N/A |
| `contentItemService()` | `publish` | ✓ | ✓ | ✓ (baseline) | ✓ (baseline) | ✓ (inferred) |
| `contentItemService()` | `update` | ✓ | ✓ | ✓ (baseline) | ✓ (baseline) | N/A |

All 6 assertion dimensions from the task scope are covered across the 17 tests. No gaps in covered operations.

**Not covered (accepted gaps — already deferred):** archive, unarchive, delete, restore — all fail-closed with `UnsupportedOperationException` before reaching the delegate, so they inherently produce no side effects. Testing them would be tautological until their permission vocabulary is defined.

---

## 3. Index Observability Assessment

**Q7: Is the index assertion strong enough?**

The test `indexNotUpdatedAfterDeniedPublish` asserts:
```java
assertThat(runtime.coreRuntime().recordedEvents()).hasSize(baselineEventCount);
```

With the comment: "Index updates are driven exclusively by domain events via the module event dispatcher. If no new events were recorded, the ContentItemPublishedIndexingSubscriber was never invoked."

**Assessment: Inference is architecturally valid.** Trace through the event pipeline:

1. `SecuredContentItemService.publish()` calls `requireGranted()` → on deny, throws `AccessDeniedException`, delegate never reached
2. The delegate (`EventPublishingContentItemService`) is the sole source of `ContentItemPublishedEvent`
3. `DeferredEventDispatcher` flushes events to BOTH the core's `EventRecorder` AND the module dispatcher (via forwarding lambda)
4. `LocalCodexEventDispatcher` delivers to `ContentItemPublishedIndexingSubscriber`
5. `recordedEvents()` captures events from the core recorder — shared origin with module dispatch

∴ `recordedEvents()` unchanged ⇒ no events emitted ⇒ no index subscriber invoked ⇒ no index mutation.

No direct `IndexWriter` bypass path exists in the `ConciliumRuntime.secured()` factory — `IndexRuntime` wraps with `ObservingIndexWriter` but writes only occur through the event subscriber.

**Recommendation:** Accept as-is. A direct index stub read would be preferable but requires exposing `IndexRuntime.getWriter()` as an accessor — beyond the scope of CODEX-017. The task should be tracked as a near-future improvement when a public index query surface exists.

---

## 4. Test Quality Assessment

### Design patterns

| Pattern | Usage | Assessment |
|---------|-------|------------|
| `@Nested` per scenario family | 4 groups with `@DisplayName` | ✓ Follows codex-custos convention |
| Switchable snapshot via `AtomicReference` | `activeSnapshot` starts `SUPER_ADMIN`, flips to `EMPTY` | ✓ Clean; avoids runtime-per-test overhead |
| Single runtime per test | `@BeforeEach` creates one `secured(activeSnapshot::get)` | ✓ Efficient; all operations share same pipeline |
| Baseline capture | Event/audit counts at end of `@BeforeEach` after setup | ✓ Robust against future setup changes |
| Top-level accessors for domain ops | `runtime.siteService()` etc. | ✓ Task rule complied |
| Core bypass for inspection only | `runtime.coreRuntime()...` for `findAll`, `findByKey`, `recordedEvents`, `findAll` on repos | ✓ Task rule complied |
| Behavior-only assertions | No `instanceof SecuredSiteService`, no internal class references | ✓ Not coupled to implementation |
| AssertJ throughout | `assertThat`, `assertThatThrownBy` | ✓ Project convention |

### Test naming

All 17 tests have `@DisplayName` describing observable behavior. Nested groups mirror the task's scenario families. ✓

### Test isolation within ConcurrentTestSuite

Note: `ConciliumRuntimeDeniedSideEffectsTest` runs in the same JVM as `ConciliumRuntimeSecuredTest` but uses `MemoryRepository` instances (per-ConciliumRuntime), so there's no state leakage. The in-memory nature of the runtime makes tests truly isolated. ✓

### Coverage gaps (intentional)

- No tests for denied archive/unarchive/delete/restore — fail-closed at the decorator level before reaching delegate; testing them produces no additional signal
- No denied `findByKey` tests — read operations that do gate on `canRead` are tested in `SecuredContentItemServiceAuthorizationMatrixTest` (custos tests)
- No cross-request cache consistency tests — Cache foundation exists for `ContentItemService` but denied operations don't reach `CachingContentItemService` (the secured decorator sits above it) ✓

---

## 5. Review Questions — All Answers

| # | Question | Verdict | Evidence |
|---|----------|---------|----------|
| 1 | Denied operations use top-level accessors? | PASS | All ops via `runtime.siteService()` etc. |
| 2 | coreRuntime() used only for inspection? | PASS | Only for `findAll`, `findByKey`, `recordedEvents()`, `findAll()` repos |
| 3 | Denied site create proves all 4 dimensions? | PASS | 4 tests: exception, state, events, audit |
| 4 | Denied content type create proves all 4? | PASS | 4 tests: exception, state, events (baseline), audit (baseline) |
| 5 | Denied publish proves all 6? | PASS | 5 tests: exception, draft status, null revision, events (baseline), audit (baseline), index (inferred) |
| 6 | Denied update proves all 4? | PASS | 4 tests: exception, revision unchanged, events (baseline), audit (baseline) |
| 7 | Index assertion strong enough? | WARN | Transitively valid via event chain; direct read preferred when available |
| 8 | Baselines captured after authorized setup? | PASS | `@BeforeEach` captures at end of setup, before `switchToUnauthorized()` |
| 9 | Behavior-focused, not coupled to internals? | PASS | No `instanceof` on internal classes; uses public interface types |
| 10 | No production code changes? | PASS | `git status` — only `ConciliumRuntimeDeniedSideEffectsTest.java` added |
| 11 | Existing secured/unsecured tests intact? | PASS | 40 pre-existing tests continue passing |
| 12 | Accepted gaps not expanded? | PASS | No new operations tested beyond Phase 1.4 scope |

---

## 6. No Production Code Changes Confirmed

`git status` confirms the only new file is `ConciliumRuntimeDeniedSideEffectsTest.java`. No pom.xml, module-info, Java source, or configuration files were modified. The existing secured decorator chain (`SecuredSiteService`, `SecuredContentTypeService`, `SecuredContentItemService`) already enforces the require-before-delegate invariant correctly — no bugs required fixes.

---

## 7. Accepted Gaps (no change from prior phases)

- Collection reads (`findAll`, `findByContentType`, `findBySiteKey`) pass through without authorization
- `findByAlias` lacks alias-to-SiteKey authorization
- `delete`, `restore`, `unarchive` are fail-closed pending permission vocabulary
- Direct actor `PermissionGrant` support and explanation trace are pending
- Direct index stub read test is pending (blocked on public `IndexRuntime` query surface)
- Audit/authorization integration is pending

None expanded by CODEX-017.

---

## 8. Follow-Up Recommendations

| Priority | Item |
|----------|------|
| Near-future | Add direct index stub read test when `IndexRuntime` exposes a public writer/reader accessor — currently `IndexWriter` instances are internal to the runtime |
| Low | Consider extracting the switchable-snapshot pattern (`AtomicReference<PermissionResolutionSnapshot>` + `switchToUnauthorized()`) into a shared test utility for future denied-operation test suites |

---

## 9. Recommendation

**Accept CODEX-017.** No changes required.

All acceptance criteria are satisfied:
- [x] Tests prove denied operations do not mutate state
- [x] Tests prove denied operations do not emit domain events
- [x] Tests prove denied publish does not update index (via event-driven inference)
- [x] Tests prove denied operations do not create Chronicon audit records
- [x] Existing secured runtime tests continue passing
- [x] Existing unsecured runtime tests continue passing
- [x] No authorization semantics changed
- [x] No production code changed
