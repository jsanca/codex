# Cache Invalidation Strategy

This document defines the Codex cache invalidation model before cache decorators or
invalidation subscribers are introduced. The goal is to clarify what can be cached safely,
what must be invalidated, and which domain events should eventually drive invalidation.

---

## 1. Cache Philosophy

Cache is not canonical storage. Canonical services and repositories remain the source of truth.

Cache accelerates identity reads and repeated misses. It must never change domain semantics,
bypass lifecycle services, or become a write surface.

Key rules:

- Cache sits in decorators or projection/read sources, never inside canonical domain logic.
- Cache invalidation is driven by domain events, not by in-place mutation.
- Cache is optional: a `NoOpCacheRegion` must be a valid substitute in any context.
- Cache state is always derivable from canonical storage on a cold start.

Intended future shapes:

```
CachingContentItemService
  -> CodexContentItemService (canonical, source of truth)

or

CachedContentItemProjectionReader
  -> ContentItemProjectionReader (read-only projection contract)
```

Neither is implemented yet. This document captures the design intent.

---

## 2. Three-State Cache Model

Codex uses a three-state cache model to distinguish a real miss from a cached absence:

| `get(key)` result                   | Meaning                                        |
|-------------------------------------|------------------------------------------------|
| `Optional.empty()`                  | Cache miss — canonical source not yet consulted |
| `Optional.of(CacheEntry.Found(v))`  | Positive hit — value is available               |
| `Optional.of(CacheEntry.NotFound())`| Negative hit — canonical source confirmed absent |

**Why `CacheEntry.NotFound` exists:**

Repeated requests for missing resources should not repeatedly hit canonical storage.
When a resource is later created, an event-driven invalidation subscriber evicts the
`NotFound` entry so the next request loads the real value.

This mirrors the dotCMS negative cache pattern and is a first-class feature of the
`CacheRegion<K, V>` abstraction.

---

## 3. Immutable Snapshots vs Mutable Pointers

The most important architectural rule for cache invalidation:

> **Immutable snapshots can be cached by id with little or no invalidation.**
> **Mutable aggregates and mutable pointers require event-driven invalidation.**

### Snapshot-like objects (stable after creation or activation)

| Cache key                | Cached value          | Invalidation need |
|--------------------------|-----------------------|-------------------|
| `ContentRevisionId`      | `ContentRevision`     | None — revisions are append-only and immutable after creation |
| `ContentTypeVersionId`   | `ContentTypeVersion`  | None — versions are immutable after activation |

Revisions and versions are never mutated in place. When content changes, a new revision or
version is created and the parent pointer is updated. Caching by id is therefore safe.

### Mutable aggregate / pointer objects (require invalidation)

| Cache key                                    | Cached value       | Why it changes                                 |
|----------------------------------------------|--------------------|------------------------------------------------|
| `SiteKey → Site`                             | `Site`             | Status transitions (started, suspended, archived) |
| `SiteId → Site`                              | `Site`             | Same — pointer to the same mutable aggregate    |
| `(SiteKey, ContentTypeKey) → ContentType`    | `ContentType`      | Activation changes published version pointer    |
| `(SiteKey, ContentTypeKey, ContentItemKey) → ContentItem` | `ContentItem` | Publish changes revision pointers |
| `ContentItemId → ContentItem`                | `ContentItem`      | Same — pointer to the same mutable aggregate    |
| Latest active version pointer                | `ContentTypeVersionId` | Changes on each activation                 |
| Current published revision pointer           | `ContentRevisionId`   | Changes on each publish                     |
| Current working revision pointer             | `ContentRevisionId`   | Changes on each new revision                |

**Rule:** Do not invalidate immutable snapshots unless the system explicitly supports mutation
of those snapshots. Invalidate the mutable pointers that decide which snapshot is current.

---

## 4. ContentType / ContentTypeVersion Invalidation Strategy

### Intended lifecycle

Schema changes produce a draft, and activation promotes the draft to a new `ContentTypeVersion`.
The `ContentType` aggregate holds a pointer to the current published version. This means:

- `ContentTypeVersion` is immutable after activation — cache by id is safe.
- `ContentType` is mutable — its published-version pointer changes on each activation.

### Safe to cache

```
ContentTypeVersionId → ContentTypeVersion
```

Potentially long TTL or stable cache with no event-driven eviction needed.

### Requires invalidation

```
(SiteKey, ContentTypeKey) → ContentType
Latest active version pointer by (SiteKey, ContentTypeKey)
Draft schema pointer (if introduced in the future)
```

### Events and their expected invalidation effect

| Event                      | Keys to evict                                    | Notes                                              |
|----------------------------|--------------------------------------------------|----------------------------------------------------|
| `ContentTypeCreatedEvent`  | Negative cache for `(SiteKey, ContentTypeKey)`   | New type may satisfy a previous 404 miss           |
| `ContentTypeActivatedEvent`| `ContentType` by key; latest active version pointer | Do not evict old `ContentTypeVersion` by id — it is immutable |
| `ContentTypeArchivedEvent` | `ContentType` by key; latest active version pointer | Type is no longer accessible via normal reads   |
| Field added / draft updated (future) | `ContentType` by key if draft state is cached | Only relevant if draft schema is cached       |
| `ContentTypeDeletedEvent` (future) | All keys for this type; put `NotFound` after eviction | |

---

## 5. ContentItem / ContentRevision Invalidation Strategy

### Intended lifecycle

Publishing does not mutate a `ContentRevision`. It updates the `ContentItem` pointer to the
newly published revision. This means:

- `ContentRevision` is immutable after creation — cache by `ContentRevisionId` is safe.
- `ContentItem` is mutable — its published and working revision pointers change on lifecycle events.

### Safe to cache

```
ContentRevisionId → ContentRevision
```

Potentially long TTL or stable cache with no event-driven eviction needed.

### Requires invalidation

```
(SiteKey, ContentTypeKey, ContentItemKey) → ContentItem
ContentItemId → ContentItem
Current published revision pointer
Current working revision pointer
Rendered/page cache (future)
Search/index projections (handled by codex-index subscriber, not cache)
```

### Events and their expected invalidation effect

| Event                          | Keys to evict                                              | Notes                                              |
|--------------------------------|------------------------------------------------------------|----------------------------------------------------|
| `ContentItemCreatedEvent`      | Negative cache for item by key; item by id if present      | New item may satisfy a previous 404 miss           |
| `ContentItemPublishedEvent`    | `ContentItem` by key and id; published revision pointer    | Do not evict `ContentRevision` by id — it is immutable. Future: evict rendered fragments/pages |
| `ContentItemArchivedEvent` (future) | `ContentItem` by key and id; published revision pointer | Item no longer visible via normal reads        |
| `ContentItemUnpublishedEvent` (future) | `ContentItem` by key and id; published revision pointer | Pointer cleared                              |
| `ContentItemDeletedEvent` (future) | All keys for this item; optionally put `NotFound`      |                                                    |
| `ContentRevisionCreatedEvent` (future) | Working revision pointer                           | New working revision available                     |

---

## 6. Negative / 404 Cache Invalidation

The negative cache is a first-class cache state. The full flow for a missing resource:

```
1. Request arrives for /blog/missing.
2. CacheRegion.get(key) → Optional.empty()   — cache miss.
3. Canonical read (service or projection reader) returns Optional.empty().
4. CacheRegion.put(key, CacheEntry.NotFound()) — store the 404.
5. Repeated requests → CacheRegion.get(key) → Optional.of(NotFound) — no canonical hit.
6. Later: ContentItemCreatedEvent fires for /blog/missing.
7. Invalidation subscriber calls CacheRegion.evict(key).
8. Next request → cache miss → canonical read → CacheEntry.Found(item) stored.
```

**Rule:** Create, update, and publish events must evict both positive and negative entries for
the affected key. The eviction logic is the same regardless of what was previously cached.

**Future distributed concern:** In a cluster, a local Caffeine eviction only reaches the current
JVM. Once Redis or distributed coordination exists, invalidation events must be propagated across
all nodes via a shared message channel or shared cache layer. Local cache invalidation is
sufficient for the MVP single-node deployment.

---

## 7. Event-to-Cache Invalidation Matrix

| Event                         | Cache keys affected                              | Evict positive? | Evict negative? | Notes                                     |
|-------------------------------|--------------------------------------------------|-----------------|-----------------|-------------------------------------------|
| `SiteCreatedEvent`            | Site by `SiteKey`; site by `SiteId`              | Evict or refresh | Evict `NotFound` | New site may satisfy previous miss       |
| `SiteStartedEvent`            | Site by `SiteKey`; site by `SiteId`              | Evict            | No              | Status changed                            |
| `SiteSuspendedEvent`          | Site by `SiteKey`; site by `SiteId`              | Evict            | No              | Status changed                            |
| `SiteArchivedEvent`           | Site by `SiteKey`; site by `SiteId`              | Evict            | No              | Status changed                            |
| `SiteUnarchivedEvent`         | Site by `SiteKey`; site by `SiteId`              | Evict            | No              | Status changed                            |
| `ContentTypeCreatedEvent`     | ContentType by `(SiteKey, ContentTypeKey)`        | Evict or refresh | Evict `NotFound` | New type may satisfy previous miss       |
| `ContentTypeActivatedEvent`   | ContentType by key; latest active version pointer | Evict            | Rarely relevant | Do not evict immutable `ContentTypeVersion` by id |
| `ContentTypeArchivedEvent`    | ContentType by key; latest active version pointer | Evict            | No              | Type no longer accessible                 |
| `ContentItemCreatedEvent`     | ContentItem by key and id                         | Evict or refresh | Evict `NotFound` | New item may satisfy previous miss       |
| `ContentItemPublishedEvent`   | ContentItem by key and id; published revision pointer | Evict        | Evict `NotFound` | Do not evict immutable `ContentRevision` by id. Future: evict rendered fragments |

---

## 8. Observance Phase 1 — Cache Metrics (Tasks 88–89)

`ObservingCacheRegion<K, V>` (in `codex.fundamentum.api.cache`) is a `CacheRegion` decorator
that records framework-neutral counters via `Observance`. It wraps any `CacheRegion`
implementation without changing its semantics.

**Counters recorded** (region name scoped, e.g. `contentItem`):

| Metric name                            | When incremented                                        |
|----------------------------------------|---------------------------------------------------------|
| `cache.{region}.get.hit`               | `get` returned a cached entry                           |
| `cache.{region}.get.miss`              | `get` returned empty (key not cached)                   |
| `cache.{region}.getOrLoad.hit`         | `getOrLoad` returned a cached entry (loader not called) |
| `cache.{region}.getOrLoad.miss`        | `getOrLoad` invoked the loader                          |
| `cache.{region}.put`                   | `put` completed                                         |
| `cache.{region}.evict`                 | `evict` completed                                       |
| `cache.{region}.clear`                 | `clear` completed                                       |

Counters are incremented only after the delegate operation succeeds. A loader or delegate that
throws does not increment any counter.

**Runtime wiring:** `CodexRuntime.assemble()` wraps the `ConcurrentMapCacheRegion` backing
`CachingContentItemService` with `ObservingCacheRegion("contentItem", observance)`. The same
reference is passed to all seven cache invalidation subscribers, so evict calls are observable
through the same `Observance` instance.

**Region name rule:** The `regionName` argument must be a stable, low-cardinality string
— never a cache key, id, or any value derived from user input or request context.

**Not in Phase 1:** TTL policies, histograms/percentiles, Caffeine stats integration,
Micrometer/Prometheus/OpenTelemetry adapters, distributed cache metrics.

---

## 9. Near-Future Implementation Plan

The recommended sequence once cache invalidation is explicitly tasked:

```
Task 44: ContentItem cache keys and CachingContentItemService (identity reads only) — DONE
Task 45: ContentItem cache invalidation subscriber — DONE
         ContentItemCreatedEvent and ContentItemPublishedEvent now have cache invalidation
         subscribers for ContentItem identity reads (ContentItemCreatedCacheInvalidationSubscriber,
         ContentItemPublishedCacheInvalidationSubscriber in codex.codex.internal.cache).
Task 46: ContentType cache keys and CachingContentTypeService (identity reads only)
Task 47: ContentType cache invalidation subscriber
Task 48: ContentRevision / ContentTypeVersion snapshot cache (discussion or implementation)
```

`ContentItem` should be first because publish already fires `ContentItemPublishedEvent`,
making the invalidation path straightforward, and published content reads are the hottest path.

For each cache decorator task, the pattern will be:

```
1. Define typed cache key (record in api or internal cache package — TBD per open questions).
2. Implement CachingXxxService or CachedXxxProjectionReader wrapping the real implementation.
3. Add unit tests for cache hit, miss, and NotFound paths.
4. Implement invalidation subscriber listening to the relevant domain event.
5. Add unit tests for invalidation subscriber.
6. Wire into ConciliumRuntime or a future cache assembly layer.
```

---

## 10. Future-Forward Topics

These belong to future tasks. Do not implement.

| Topic                               | Notes                                                          |
|-------------------------------------|----------------------------------------------------------------|
| TTL policies                        | `CachePolicy` abstraction — `Found` TTL vs `NotFound` TTL      |
| `Found` → longer TTL, `NotFound` → shorter TTL | E.g., 5 min vs 30 sec; policy not yet modeled    |
| `Caffeine.expireAfterWrite`         | Needs a `CachePolicy` or explicit duration parameter           |
| `Caffeine.refreshAfterWrite`        | Eager refresh / refresh-ahead for hot cache regions            |
| Redis shared cache                  | L2 distributed cache; `CacheRegion` adapter needed             |
| Chronicle Map local persistent cache| Off-heap; viable for large content-item sets                   |
| Distributed invalidation            | Requires cluster coordination (future `codex-concordia`)        |
| Cache metrics / tracing             | Caffeine stats API; Micrometer integration                     |
| Cache warmers / scheduled preload   | Background population before traffic hits                      |
| Cache region naming                 | Named regions for observability and scoped eviction            |
| Configuration subsystem             | Max size, TTL, refresh policy per region via config            |
| Spring Cache integration            | `@Cacheable` / `@CacheEvict` — only in adapter modules         |

---

## 11. Open Questions

These are not resolved here. Do not implement until a task explicitly addresses each one.

1. **Should `ContentTypeVersion` and `ContentRevision` be formally declared immutable?**
   The domain model currently treats them as effectively immutable after creation/activation,
   but no formal immutability contract exists. A formal declaration would enable safe
   unbounded caching by id.

2. **Snapshot cache TTL: very long, no TTL, or event-based only?**
   If versions and revisions are truly immutable, event-based eviction may never be needed.
   Should these snapshot caches use infinite TTL (no expiry), very long TTL as a safety net,
   or rely purely on explicit eviction?

3. **Should `NotFound` entries have a shorter TTL once configuration exists?**
   A newly created resource should become visible quickly after creation. Shorter `NotFound`
   TTL reduces the window where stale 404s are served if an invalidation event is missed.

4. **Where do cache invalidation subscribers live?**
   Options: `codex-codex` (access to domain events and repositories), `codex-concilium`
   (composition layer), or a dedicated future cache module. The cleanest option is likely
   a cache module that depends on `codex-codex` events and injects into `ConciliumRuntime`.

5. **Cache decorators around services or projection readers?**
   `CachingContentItemService` wraps write+read, adding complexity. `CachedContentItemProjectionReader`
   wraps read-only access and is simpler to reason about. The projection reader path may be
   preferable for the first iteration.

6. **Distributed invalidation model?**
   Local Caffeine eviction only reaches the current JVM. Options: publish invalidation events
   to a Redis Pub/Sub channel, use a shared Redis cache as L2, or rely on short TTLs to
   bound staleness. This depends on whether `codex-concordia` or a Redis adapter lands first.

7. **Cache key design: records in `api` or internal cache packages?**
   Typed cache keys (e.g., `ContentItemCacheKey`) could live in `codex.codex.api.cache` for
   reuse by different adapter implementations, or in internal cache packages if they are
   implementation details. Decision deferred until the first cache decorator task.
