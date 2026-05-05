# Task42: Caffeine CacheRegion Adapter

## Objective

Introduce the first real cache adapter for Codex using Caffeine.

Codex already has a framework-agnostic cache foundation in `codex-fundamentum`:

- `CacheEntry<V>`
- `CacheRegion<K, V>`
- `NoOpCacheRegion<K, V>`
- `ConcurrentMapCacheRegion<K, V>`
- `RecordingCacheRegion<K, V>`

This task adds a Caffeine-backed implementation of `CacheRegion<K, V>`.

The goal is to provide a production-grade local in-memory cache adapter while preserving the existing Codex cache abstraction.

Do not integrate this cache into `ContentItemService` yet.

Do not introduce cache decorators yet.

Do not introduce cache invalidation subscribers yet.

This task is only the adapter.

## Decision Context

Codex cache design supports three cache states:

```text
Optional.empty()
  -> real cache miss

Optional.of(CacheEntry.Found(value))
  -> positive cache hit

Optional.of(CacheEntry.NotFound())
  -> negative / 404 cache hit
````

This supports the dotCMS-style negative cache strategy:

```text
missing resource requested
  -> canonical lookup says not found
  -> cache stores CacheEntry.NotFound
  -> repeated requests avoid hitting canonical storage
  -> future creation/update event evicts the negative entry
  -> next request loads the real value
```

Caffeine is a strong fit for Codex local cache because it provides:

* high-performance local memory caching
* bounded cache size
* compute-if-absent style loading
* eviction policies
* future TTL support
* future refresh-after-write support

For this task, keep the first adapter simple.

## Scope

Implement in `codex-fundamentum` or an appropriate cache adapter package:

* Caffeine dependency
* `CaffeineCacheRegion<K, V>`
* tests

Do not implement:

* service cache decorators
* `CachingContentItemService`
* cache invalidation subscribers
* Redis adapter
* Chronicle Map adapter
* TTL policy abstraction
* eager refresh / refresh-ahead
* scheduled cache warmers
* distributed cache
* cache metrics
* cache tracing
* Spring integration
* runtime wiring
* configuration subsystem

## Dependency Location

Add Caffeine dependency where the cache adapter lives.

Preferred location for now:

```text
codex-fundamentum
```

Reason:

`CacheRegion` currently lives in `codex-fundamentum`, and this first adapter can remain close to the cache abstraction.

However, keep the implementation clean and avoid allowing `fundamentum` to become a god module.

Only add Caffeine.

Do not add Spring Cache.

Do not add Redis.

Do not add Micrometer.

Do not add configuration libraries.


## Package Location

Suggested package:

```text
codex.fundamentum.api.cache
```

Create:

```text
codex.fundamentum.api.cache.CaffeineCacheRegion
```

Alternative:

```text
codex.fundamentum.internal.cache.CaffeineCacheRegion
```

But because the current cache implementations appear to live under `api.cache`, keep consistency unless the project has a stricter internal convention.

If placed in `api.cache`, document that it is an adapter implementation of the public cache contract.

## 1. Create CaffeineCacheRegion

Create:

```java
public final class CaffeineCacheRegion<K, V> implements CacheRegion<K, V>
```

It should wrap:

```java
com.github.benmanes.caffeine.cache.Cache<K, CacheEntry<V>>
```

Recommended constructor/factories:

```java
public CaffeineCacheRegion(Cache<K, CacheEntry<V>> cache)

public static <K, V> CaffeineCacheRegion<K, V> unbounded()

public static <K, V> CaffeineCacheRegion<K, V> maximumSize(long maximumSize)
```

Requirements:

* constructor validates cache non-null
* `unbounded()` creates a simple Caffeine cache
* `maximumSize(...)` creates a bounded Caffeine cache
* reject maximum size less than 1
* no TTL yet
* no refresh-after-write yet
* no stats API yet unless already trivial
* no async loading
* no eviction listener
* no scheduler

Suggested implementation:

```java
private final Cache<K, CacheEntry<V>> cache;
```

## 2. Implement CacheRegion Methods

### get

```java
Optional<CacheEntry<V>> get(K key)
```

Requirements:

* reject null key
* return `Optional.empty()` when absent
* return cached `CacheEntry` when present
* support both `Found` and `NotFound`

Implementation hint:

```java
return Optional.ofNullable(cache.getIfPresent(key));
```

### getOrLoad

```java
CacheEntry<V> getOrLoad(K key, Supplier<? extends CacheEntry<V>> loader)
```

Requirements:

* reject null key
* reject null loader
* use Caffeine atomic loading semantics:

```java
cache.get(key, ignored -> ...)
```

* validate loader result non-null
* store `CacheEntry.NotFound` exactly like `CacheEntry.Found`
* loader should only be invoked when absent, according to Caffeine semantics

Implementation hint:

```java
return cache.get(key, ignored -> {
    final CacheEntry<V> entry = loader.get();
    return Objects.requireNonNull(entry, "loader must not return null");
});
```

### put

```java
void put(K key, CacheEntry<V> entry)
```

Requirements:

* reject null key
* reject null entry
* put entry into cache

### evict

```java
void evict(K key)
```

Requirements:

* reject null key
* invalidate key

### clear

```java
void clear()
```

Requirements:

* invalidate all entries

## 3. Negative Cache Behavior

Add tests proving Caffeine stores and returns `CacheEntry.NotFound`.

Required behavior:

```text
get missing key
  -> Optional.empty()

getOrLoad missing key with NotFound loader
  -> returns NotFound
  -> stores NotFound

get same key after NotFound stored
  -> Optional.of(NotFound)

getOrLoad same key after NotFound stored
  -> does not call loader again
  -> returns cached NotFound

evict key
  -> removes NotFound
```

This mirrors the behavior already expected from `ConcurrentMapCacheRegion`.

## 4. Bounded Cache Behavior

If `maximumSize(...)` is implemented, add a simple test proving bounded construction works.

Do not write brittle eviction tests that depend on exact immediate eviction timing unless using Caffeine cleanup carefully.

Acceptable tests:

* `maximumSize(0)` rejects
* `maximumSize(-1)` rejects
* `maximumSize(10)` creates cache and basic put/get works

Avoid tests like “put 11 entries and assert first is evicted” unless reliable with Caffeine’s cleanup semantics.

## 5. No TTL Yet

Do not implement TTL in this task.

However, add JavaDoc note:

```text
Future versions may support different TTL policies for Found and NotFound entries.
```

Do not introduce:

* `CachePolicy`
* `CacheConfiguration`
* `Duration`
* `expireAfterWrite`
* `expireAfterAccess`

unless strictly needed by Caffeine construction, which it should not be.

## 6. No Eager Refresh Yet

Do not implement refresh-ahead / eager refresh in this task.

Add JavaDoc note if useful:

```text
Future versions may use Caffeine refreshAfterWrite or scheduled warmers for eager cache regions.
```

Do not introduce:

* `LoadingCache`
* `AsyncLoadingCache`
* `CacheRefresher`
* `CacheRefreshPolicy`
* scheduled executor
* background threads

## 7. Tests

Add JUnit 5 tests.

Suggested test file:

```text
codex-fundamentum/src/test/java/codex/fundamentum/api/cache/CaffeineCacheRegionTest.java
```

No Spring.

No Mockito.

Test cases:

### Constructor/factory tests

* constructor rejects null cache
* `unbounded()` creates usable cache
* `maximumSize(1)` creates usable cache
* `maximumSize(0)` rejects
* `maximumSize(-1)` rejects

### Basic behavior tests

* `get` returns empty when key missing
* `put` then `get` returns found entry
* `put` supports `NotFound`
* `evict` removes entry
* `clear` removes all entries

### getOrLoad tests

* `getOrLoad` loads and stores found entry
* `getOrLoad` loads and stores not-found entry
* `getOrLoad` does not call loader again when found entry exists
* `getOrLoad` does not call loader again when not-found entry exists
* `getOrLoad` rejects null key
* `getOrLoad` rejects null loader
* `getOrLoad` rejects null loader result

### Null guard tests

* `get` rejects null key
* `put` rejects null key
* `put` rejects null entry
* `evict` rejects null key

### Optional concurrency test

Optional only if easy and non-flaky:

* multiple concurrent calls for the same key should not call loader repeatedly

If implemented, keep it simple and stable.

Do not add timing-sensitive tests.

## 8. module-info.java

Update `codex-fundamentum/src/main/java/module-info.java`.

Add the required Caffeine module.

The Caffeine automatic/module name may be:

```java
requires com.github.benmanes.caffeine;
```

or whatever the actual module name is according to the dependency.

Verify with the build.

Do not guess if the compiler reports the correct name.

## 9. Documentation

Update cache documentation if present.

If no cache doc exists, update a small relevant note or JavaDoc only.

Suggested README/doc note:

```text
CaffeineCacheRegion is the first production-grade local cache adapter for CacheRegion.
It supports Found and NotFound cache entries.
TTL, refresh-ahead, Redis, and Chronicle Map remain future work.
```

Do not create a large ADR unless existing project convention requires it.

## 10. Acceptance Criteria

Task is complete when:

* Caffeine dependency is added
* `CaffeineCacheRegion` exists
* it implements `CacheRegion`
* it supports `Found` and `NotFound`
* it supports `getOrLoad` using Caffeine’s atomic `cache.get(...)`
* it supports `put`, `get`, `evict`, and `clear`
* tests pass
* no TTL is introduced
* no refresh-ahead is introduced
* no service cache decorators are introduced
* no cache invalidation subscriber is introduced

## 11. Maven Commands

Run:

```bash
mvn test -pl codex-fundamentum
```

If practical, also run:

```bash
mvn compile
```

Report command results.

## 12. Post-Task Report

After implementation, report:

* files created
* files modified
* dependency changes
* module-info changes
* tests added
* Maven commands run
* whether tests passed
* intentional deviations
* open questions
* recommended follow-up tasks

## 13. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Keep cache framework-agnostic at the interface level.
* Do not change `CacheRegion`.
* Do not change `CacheEntry`.
* Do not add TTL yet.
* Do not add refresh-ahead yet.
* Do not add Redis.
* Do not add Chronicle Map.
* Do not add service decorators.
* Do not add cache invalidation subscribers.
* Do not add runtime wiring.
* Do not add Spring.
* Do not add persistence.
* Do not add search.
* Do not add workflow.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.
* Prefer small, explicit, testable adapter code.
