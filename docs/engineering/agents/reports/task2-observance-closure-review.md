# Observance Closure Review — Report

**Date:** 2026-05-17
**Review scope:** `CodexRuntime`, all `Timed*` decorators, event dispatchers, cache dispatcher, index writer, metric naming, no-op paths, tests, blind spots.

---

## 1. Observance Propagation: Consistent

The same `Observance` instance flows through all layers without duplication or gaps:

```
ConciliumRuntime.inMemory(observance)
├── CodexRuntime.inMemory(externalDispatcher, observance)
│   └── assemble(externalDispatcher, observance)
│       ├── cacheDispatcher     ← LocalCodexEventDispatcher(subscribers, observance)  ✓
│       ├── DeferredEventDispatcher(composite, asyncExecutor, observance)             ✓
│       ├── TimedSiteService(eventPublishingSiteService, observance)                  ✓
│       ├── TimedContentTypeService(eventPublishingCTService, observance)             ✓
│       └── TimedContentItemService(eventPublishingCIService, observance)             ✓
├── IndexRuntime.inMemory(projectionReader, observance)
│   └── ObservingIndexWriter(indexWriter, observance)                                 ✓
└── moduleDispatcher ← LocalCodexEventDispatcher(allSubscribers, observance)          ✓
```

**Verdict: Clean.** One instance injected at the top by `ConciliumRuntime` and threaded through every instrumented component. No `Observance` leaks to non-instrumented components (repositories, `ChroniconRuntime`, `EventRecorder`).

---

## 2. No-Op Factory Paths: No Accidental Gaps

| Entry point | No-op path | Uses `Observance.noop()`? |
|---|---|---|
| `CodexRuntime.inMemory()` | → `inMemory(event -> {}, Observance.noop())` | ✓ |
| `CodexRuntime.inMemory(CodexEventDispatcher)` | → `inMemory(externalDispatcher, Observance.noop())` | ✓ |
| `ConciliumRuntime.inMemory()` | → `inMemory(Observance.noop())` | ✓ |
| `ConciliumRuntime.compose(...)` | → `new LocalCodexEventDispatcher(allSubscribers)` (no-op default) | ✓ |
| `DeferredEventDispatcher(delegate, asyncExecutor)` | → `this(delegate, asyncExecutor, Observance.noop())` | ✓ |
| `LocalCodexEventDispatcher(subscribers)` | → `this(subscribers, Observance.noop())` | ✓ |

**Verdict: Clean.** Every no-op path delegates to `Observance.noop()` consistently. No component silently falls back to null or stays uninstrumented when an Observance is available.

---

## 3. Metric Naming Consistency

| Component | Namespace | Example |
|---|---|---|
| `TimedSiteService` | `services.site.{op}.{metric}` | `services.site.create.duration` |
| `TimedContentTypeService` | `services.contentType.{op}.{metric}` | `services.contentType.create.duration` |
| `TimedContentItemService` | `services.contentItem.{op}.{metric}` | `services.contentItem.create.duration` |
| `ObservingIndexWriter` | `index.{op}.{metric}` | `index.upsert.calls` |
| `DeferredEventDispatcher` | `deferred.*` | `deferred.events.committed` |
| `LocalCodexEventDispatcher` | `events.*` / `subscribers.*` | `events.dispatched.{EventName}` |

**Prefix consistency within service layer:** All three `services.*` decorators use identical `{operation}.duration` / `{operation}.failed` suffixes. ✅

**Remaining prefix inconsistency:**
- `services.site.*` uses `services.` prefix; `index.*` does not
- `deferred.commit.duration` vs `deferred.events.discardedOnRollback` — different hierarchy depth

**Severity: Low.** These are cosmetic differences. Operators can group by prefix in monitoring dashboards. Not worth renaming without a broader naming standard.

**Suffix convention:** All modules now use `.failed` (singular). Previously `index.*` used `.failures` (plural) — fixed in Task 1 review. ✅

---

## 4. Cardinality Assessment

Dynamic metric names (grow with event types and subscriber types):

| Source | Names per unit | Current count | Growth factor |
|---|---|---|---|
| `events.dispatched.{EventName}` | 1 per event type | ~20 | Each new event type |
| `subscribers.*.{SubscriberName}` | 3 per subscriber | ~26 × 3 = 78 | Each new subscriber type |
| `deferred.events.buffered.{EventName}` | 1 per event type | ~20 | Each new event type |
| `deferred.events.dispatchedImmediately.{EventName}` | 1 per event type | ~20 | Each new event type |

**Fixed metric names (bounded):** 62 total across all `MetricNames` constants.

**Dynamic names:** ~138 total at current code size. With projected growth (50 event types × 50 subscribers), this could reach ~350 time series.

**Risk: Medium.** Class `getSimpleName()` is brittle — refactoring a class name changes the metric name, breaking monitoring continuity. Subscriber names leak internal class structure.

**Mitigation (future-forward):** Add a `metricName()` or `metricLabel()` method to `CodexEvent` and `CodexEventSubscriber` interfaces so names are decoupled from class identity. Not urgent.

---

## 5. Test Coverage

| Test class | Module | Scope | Tests |
|---|---|---|---|
| `TimedSiteServiceTest` | codex-codex | All 8 operations: success, failure, propagation | ~25 |
| `TimedContentTypeServiceTest` | codex-codex | All 8 operations: success, failure, runtime integration | ~30 |
| `TimedContentItemServiceTest` | codex-codex | All 10 operations: success, failure, void methods, runtime integration | ~35 |
| `CodexRuntimeObservanceTest` | codex-codex | Runtime wiring: durations, failures, no-op path | 10 |
| `ObservingIndexWriterTest` | codex-index | Upsert/delete: calls, durations, failures, exception propagation | 14 |
| `IndexMetricNamesTest` | codex-index | Constant value assertions | 6 |
| `ConciliumRuntimeTest` | codex-concilium | End-to-end publish/archive flow: all layers | ~10 |
| `DeferredEventDispatcherTest` | codex-codex | Buffered, committed, discarded, immediate | ~10 |
| `LocalCodexEventDispatcherTest` | codex-fundamentum | Subscriber invocation, failure counters | ~6 |

**Coverage assessment: Good.** All decorator operations are tested for duration recording, failure counting, and exception propagation. Runtime integration tests verify that `CodexRuntime.inMemory(observance)` and `ConciliumRuntime.inMemory(observance)` wire Observance through the full stack.

**Coverage gaps:**
- No dedicated test for cache invalidation dispatcher Observance (metrics like `subscribers.invoked.ContentItemCreatedCacheInvalidationSubscriber` are not asserted anywhere). The wiring is correct (verified by reading `assemble()`), but there's no assertion proving it emits metrics.
- No test verifying that `TimedContentItemService` emits metrics for void-returning `delete()`.

---

## 6. Remaining Blind Spots

| Component | Instrumented? | Reason / Note |
|---|---|---|
| `EventRecorder` | No | By design — testing helper, not production |
| `CachingContentItemService` | No | Sits between Timed and EventPublishing in the stack. Cache hit/miss is invisible. Timed measures the full operation including cache lookup. |
| `ContentRevisionService` | No | No `TimedContentRevisionService` exists. Service boundary is internal (used by ContentItemService, not exposed at runtime). |
| `CodexRuntime.shutdown()` | No | Shutdown metrics (duration, executor drain) would require a separate concern. |
| `ChroniconRuntime` | No | By design — pure audit projection, no timed operations. |
| `MemoryXxxRepository` methods | No | By design — repositories are dumb CRUD. |
| `CodexExecutor.submit()` | No | Async dispatch is measured by `DeferredEventDispatcher` at the policy level, not the executor level. |

**Severity: Low.** The only actionable gap is `CachingContentItemService` — cache hit/miss ratio is operationally useful. All other gaps are either by design or untestable at current Phase 1 scope.

---

## 7. Timed Decorator Pattern: Consistent

All three `Timed*` decorators follow an identical pattern:

```java
public final class TimedXxxService implements ForwardingXxxService {
    private final XxxService delegate;
    private final Observance observance;

    public TimedXxxService(XxxService delegate, Observance observance) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.observance = Objects.requireNonNull(observance, "observance must not be null");
    }

    @Override public Result operation(Command cmd, Actor actor) {
        return timed(OP_DURATION, OP_FAILED, () -> delegate.operation(cmd, actor));
    }

    private <T> T timed(String durationKey, String failedKey, Supplier<T> action) {
        try { return observance.timer(durationKey).record(action); }
        catch (RuntimeException ex) { observance.counter(failedKey).increment(); throw ex; }
    }
}
```

`TimedContentItemService` adds a `timedVoid()` variant for the void-returning `delete()` method. This is the only structural difference.

**Verdict: Consistent.** Every `Timed*` constructor: delegate + Observance, null guards, forwarding interface, private `timed()` helper.

**Duplication note:** The `timed()` private helper is copy-pasted across 3 classes. Extraction to a shared utility is a future improvement candidate (see section 8), but not urgent given the small size (3 copies × 8 lines).

---

## 8. Answers to Task Questions

**Q1: Is the same runtime Observance instance propagated consistently?**
**A:** Yes. `ConciliumRuntime.inMemory(observance)` passes the same instance to `CodexRuntime`, `IndexRuntime`, and the module `LocalCodexEventDispatcher`. All assembled components within each runtime receive the same instance.

**Q2: Are there any places still using `Observance.noop()` accidentally?**
**A:** No. Every no-op path is intentional: no-arg factory methods explicitly delegate with `Observance.noop()`. The `ConciliumRuntime.compose()` path uses no-op for its module dispatcher (correct — the caller constructed pre-wired runtimes externally).

**Q3: Are metric names consistent across services, events, index, and cache?**
**A:** Service layer (`services.{domain}.{op}.{metric}`) is fully consistent. `index.*` lacks the `services.` prefix but uses matching suffixes. Event/dispatch names use flat `events.*` / `subscribers.*` / `deferred.*` prefixes — different but appropriate for their domain. `.failed` suffix is now uniform everywhere.

**Q4: Are there any cardinality risks?**
**A:** Medium. Subscriber and event type names use `Class.getSimpleName()`. With 50+ event types and 50+ subscribers, this produces ~350 dynamic time series. Class renames break metric continuity. Mitigation: add `metricName()` to event/subscriber interfaces (future). Not blocking.

**Q5: Are tests sufficient to prove runtime observability?**
**A:** Yes for the service layer, deferred dispatcher, and index writer. Partially for the cache invalidation dispatcher (wiring is correct but no dedicated assertion proves it emits metric names; `ConciliumRuntimeTest` touches only index/chronicon subscribers, not cache subscribers). The void-returning `delete()` path in `TimedContentItemService` is tested.

**Q6: What are the top 3 remaining Observance tasks?**
**A:**
1. **Cache invalidation Observance test** — Add 1-2 focused tests in `CodexRuntimeObservanceTest` asserting that cache invalidation subscribers emit `subscribers.invoked.ContentItemCreatedCacheInvalidationSubscriber` metrics.
2. **Extract shared `timed()` utility** — Reduce 3× copy-paste of the `try { timer.record() } catch { counter.increment() }` pattern into a static helper in `codex-fundamentum.api.observance`.
3. **Stabilize dynamic metric names** — Add a `String metricLabel()` method to `CodexEvent` and `CodexEventSubscriber` so class renames don't break monitoring. (Future-forward — document only for now.)

**Q7: Should we abstract repeated timed-service logic now, or wait?**
**A:** Wait. The 3 `Timed*` classes are small (~120 lines each) and the duplication (private `timed()` helper) is only 8 lines per class. The pattern is stable and well-understood. Extract only when a 4th Timed decorator needs to be created (e.g., `TimedContentRevisionService`) — at that point the benefit of a shared utility exceeds the cost of 3× copy-paste.

---

## 9. Summary

| Category | Assessment |
|---|---|
| Observance propagation | ✅ Consistent, single instance through full stack |
| No-op factory paths | ✅ No accidental gaps |
| Metric naming | ✅ Service layer uniform; minor prefix inconsistency with `index.*` |
| Cardinality | ⚠️ Medium risk from class-name-derived metric names |
| Test coverage | ✅ Good for services/index/dispatcher; gap in cache invalidation assertions |
| Decorator pattern | ✅ Identical across all 3 Timed* services |
| Blind spots | Low severity — `CachingContentItemService` cache hit/miss is the only actionable gap |
| Timed decorator duplication | 3× copy-paste of `timed()` helper — wait until 4th decorator before extracting |
