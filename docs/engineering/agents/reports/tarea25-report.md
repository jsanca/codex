# Task 25 Post-Task Report — Cache Foundation

## Files Changed

### New source files
- `codex-fundamentum/src/main/java/codex/fundamentum/api/cache/CacheEntry.java`
  — Sealed interface with two permitted record types: `Found<V>(V value)` and `NotFound<V>()`.
  Static factories `CacheEntry.found(value)` and `CacheEntry.notFound()`. Default `isFound()` /
  `isNotFound()` methods implemented via `instanceof` on the sealed permits. `Found` rejects null
  value in its canonical constructor.
- `codex-fundamentum/src/main/java/codex/fundamentum/api/cache/CacheRegion.java`
  — Interface with five methods: `get`, `getOrLoad`, `put`, `evict`, `clear`. Documents three-state
  semantics (miss / found / not-found), atomic loading expectation, and future adapter directions
  (Caffeine, Redis, Chronicle Map).
- `codex-fundamentum/src/main/java/codex/fundamentum/api/cache/NoOpCacheRegion.java`
  — Never caches. `get` always returns `Optional.empty()`. `getOrLoad` validates key + loader,
  calls loader on every invocation, validates result, returns it without storing.
- `codex-fundamentum/src/main/java/codex/fundamentum/api/cache/ConcurrentMapCacheRegion.java`
  — Backed by `ConcurrentHashMap`. `getOrLoad` uses `computeIfAbsent` for atomic load-and-store.
  `NotFound` entries stored like any other entry (negative caching). No TTL, no eviction policy,
  no max size.
- `codex-fundamentum/src/main/java/codex/fundamentum/api/cache/RecordingCacheRegion.java`
  — Test helper. Delegates actual storage to `ConcurrentMapCacheRegion`. Records get / load / put /
  evict keys (insertion order via `synchronized ArrayList`) and a clear counter. Exposes immutable
  snapshots via `List.copyOf`. `clearRecording()` resets recordings without disturbing cache
  contents.

### New test files
- `codex-fundamentum/src/test/java/codex/fundamentum/api/cache/CacheEntryTest.java` — 8 tests
- `codex-fundamentum/src/test/java/codex/fundamentum/api/cache/NoOpCacheRegionTest.java` — 11 tests
- `codex-fundamentum/src/test/java/codex/fundamentum/api/cache/ConcurrentMapCacheRegionTest.java` — 19 tests (includes concurrency test)
- `codex-fundamentum/src/test/java/codex/fundamentum/api/cache/RecordingCacheRegionTest.java` — 18 tests

### Modified files
- `codex-fundamentum/src/main/java/module-info.java`
  — Added `exports codex.fundamentum.api.cache;`

## Tests Run

```
mvn test -pl codex-fundamentum
```

- New cache tests: **56 passed** (8 + 11 + 19 + 18)
- Full module total: **78 passed**, 0 failures, 0 errors

## Intentional Deviations

None. Implemented exactly as specified.

## Open Questions / Architectural Notes

- **`NotFound` singleton vs new instances**: `CacheEntry.notFound()` returns `new NotFound<>()`
  each time. Since `record NotFound<V>()` has no components, all instances are `equals()` and the
  `ConcurrentHashMap` equality works correctly. A singleton could save allocations but would require
  an unchecked cast; left simple for now.
- **`RecordingCacheRegion` records `loadKeys` on every `getOrLoad` call**, even if the key was
  already cached (loader not invoked). This accurately reflects that the caller requested a load,
  regardless of whether the delegate actually called the supplier. Consistent with how `RecordingIndexWriter`
  records every `upsert` call.
- **Concurrency test**: The test asserts loader called exactly once under 20 concurrent threads,
  which `computeIfAbsent` guarantees for `ConcurrentHashMap`. Added `assertTrue >= 1` guard before
  the stricter assertion so the failure message is informative if the guarantee ever breaks.

## Recommended Follow-up Tasks

- **`CachingContentItemProjectionSource`** (near-future): wrap `RepositoryContentItemProjectionSource`
  with a `CacheRegion` decorator, matching the decorator pipeline pattern.
- **`CachingContentItemService`** (near-future): service decorator that checks cache before
  delegating to `CodexContentItemService`.
- **Cache invalidation subscribers** (near-future): `ContentItemCreatedEvent /
  ContentItemPublishedEvent / ContentItemArchivedEvent → CacheInvalidationSubscriber → CacheRegion.evict(...)`.
- **`CaffeineCacheRegion`** adapter (future-forward): bounded size, TTL, refresh-ahead.
