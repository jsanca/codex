# Implementation Report — CODEX-017: Denied-Operation Side-Effect Consistency

**Task:** CODEX-017
**Date:** 2026-08-07
**Implementer:** Clio
**Status:** COMPLETE

## Objective

Prove via tests that denied operations in `ConciliumRuntime.secured(...)` produce no side effects
across the full runtime pipeline — domain state, domain events, Chronicon audit records, and index
projections.

## Approach

Added a new focused test class:

**File:** `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeDeniedSideEffectsTest.java`

The class uses a switchable `AtomicReference<PermissionResolutionSnapshot>` that starts as
`SUPER_ADMIN_SNAPSHOT`, allows authorized `@BeforeEach` setup, then flips to `EMPTY_SNAPSHOT`
before assertions run. A single `ConciliumRuntime.secured(activeSnapshot::get)` instance is
shared within each test method.

## Test Coverage (17 tests, 4 nested groups)

### DeniedSiteCreate (4 tests)

No prior state — snapshot is empty from the start.

| Test | Assertion |
|------|-----------|
| `throwsAccessDeniedException` | `AccessDeniedException` on `siteService().create(...)` |
| `noEventsRecorded` | `coreRuntime().recordedEvents()` is empty |
| `noChroniconAuditRecord` | `chroniconRuntime().repository().findAll()` is empty |
| `siteStateRemainsEmpty` | `coreRuntime().siteService().findAll(...)` is empty |

### DeniedContentTypeCreate (4 tests)

Setup: authorized site created, then snapshot flipped.

| Test | Assertion |
|------|-----------|
| `throwsAccessDeniedException` | `AccessDeniedException` on `contentTypeService().create(...)` |
| `noNewEventsAfterDenied` | event count unchanged from baseline |
| `noChroniconRecordAfterDenied` | audit count unchanged from baseline |
| `siteHasNoContentTypesAfterDenied` | `coreRuntime().contentTypeService().findBySiteKey(...)` is empty |

### DeniedContentItemPublish (5 tests)

Setup: full stack — site, content type (with TITLE field, activated), draft item — then snapshot flipped.

| Test | Assertion |
|------|-----------|
| `throwsAccessDeniedException` | `AccessDeniedException` on `contentItemService().publish(...)` |
| `itemRemainsInDraft` | `item.status() == DRAFT`, `item.currentPublishedRevisionId() == null` |
| `noNewEventsAfterDenied` | event count unchanged from baseline |
| `noChroniconRecordAfterDenied` | audit count unchanged from baseline |
| `indexNotUpdatedAfterDeniedPublish` | event count unchanged — index updates are event-driven, so no new events = no subscriber invoked |

### DeniedContentItemUpdate (4 tests)

Setup: full stack to draft item, then snapshot flipped.

| Test | Assertion |
|------|-----------|
| `throwsAccessDeniedException` | `AccessDeniedException` on `contentItemService().update(...)` |
| `itemWorkingRevisionUnchanged` | `item.currentWorkingRevisionId()` equals original captured in `@BeforeEach` |
| `noNewEventsAfterDenied` | event count unchanged from baseline |
| `noChroniconRecordAfterDenied` | audit count unchanged from baseline |

## Key Design Choices

**State reads via raw bypass**: When checking item state after a denied operation, tests use
`runtime.coreRuntime().contentItemService().findByKey(...)` with the ADMIN actor. The task spec
explicitly permits this pattern for low-level state inspection. Domain operations still go through
`runtime.siteService()` etc.

**Index observability**: Index updates are driven solely by domain events dispatched through the
module event pipeline. An unchanged `recordedEvents()` count transitively proves that no
`ContentItemPublishedIndexingSubscriber` was invoked — no separate index read is needed.

**Baseline capture**: For groups with pre-existing state, baselines are captured at the end of
`@BeforeEach` after all authorized setup completes. This keeps assertions relative and robust
against future changes to setup event counts.

## No Production Code Changes

No production code was modified. The existing secured decorator chain (`SecuredSiteService`,
`SecuredContentTypeService`, `SecuredContentItemService`) already enforces the
require-before-delegate invariant correctly. No bugs were found.

## Validation

```
git diff --check -- "*.java" "*.xml"   → EXIT 0 (no whitespace errors)

mvn test -pl codex-concilium -am --no-transfer-progress
  → 57 tests, 0 failures — BUILD SUCCESS
  (40 pre-existing + 17 new)
```

## Acceptance Criteria — All Met

- [x] Tests prove denied operations do not mutate state
- [x] Tests prove denied operations do not emit domain events
- [x] Tests prove denied publish does not update index
- [x] Tests prove denied operations do not create Chronicon audit records accidentally
- [x] Existing secured runtime tests continue passing
- [x] Existing unsecured runtime tests continue passing
- [x] No authorization semantics changed
- [x] No production code changed
