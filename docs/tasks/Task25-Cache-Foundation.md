# Task25: Cache Foundation

## Objective

Introduce the first framework-agnostic cache foundation for Codex.

This task defines the core cache abstractions and simple in-memory/test implementations.

The cache foundation must support:

- positive cache hits
- negative / 404 cache hits
- lazy loading through `getOrLoad`
- thread-safe compute-if-absent behavior where possible
- future compatibility with Caffeine, Redis, Chronicle Map, TTL, eager refresh, and cache invalidation subscribers

This task should not implement Caffeine, Redis, Chronicle Map, TTL, refresh-ahead, cache decorators, cache invalidation subscribers, or configuration yet.

## Decision Context

Codex needs cache support, but cache must not become part of canonical domain behavior.

Canonical services remain the source of truth.

Cache accelerates identity reads and repeated misses.

Search/indexing remains separate from cache.

We want to support the dotCMS-style 404 cache strategy:

```text
1. Request for missing resource arrives.
2. Cache misses.
3. Canonical source returns not found.
4. Cache stores a negative / NotFound entry.
5. Repeated requests return cached NotFound instead of hitting canonical storage.
6. If another node later creates the resource, an event-driven invalidation removes the NotFound entry.
7. The next request loads the real value.
```

This requires distinguishing:

cache miss
cached found value
cached not found / 404

Therefore, do not use Optional<V> as the cached value model.

Use an explicit CacheEntry<V> abstraction.

Architectural Direction

Cache should be a port/abstraction.

Future implementations may include:

Caffeine
Redis
Chronicle Map
NoOp
Recording
ConcurrentMap

For now, implement only simple local/test implementations.

Cache should eventually be used by service decorators or read sources, not hardcoded into canonical domain services.

Expected future shapes:

CachingContentItemService
-> CodexContentItemService
-> Repository

or:

ContentItemProjectionSource
-> cached/read-only source
-> repository-backed source

Event-driven invalidation will come later:

ContentItemCreatedEvent / ContentItemPublishedEvent / ContentItemArchivedEvent
-> CacheInvalidationSubscriber
-> CacheRegion.evict(...)

Do not implement invalidation subscribers in this task.

Scope

Implement:

CacheEntry<V>
CacheRegion<K, V>
NoOpCacheRegion<K, V>
ConcurrentMapCacheRegion<K, V>
RecordingCacheRegion<K, V>
tests
documentation notes

Do not implement:

Caffeine
Redis
Chronicle Map
TTL
expiration
refresh-ahead / eager refresh
scheduled warmers
cache invalidation subscribers
cache decorators for services
cache configuration
distributed invalidation
cluster coordination
locks
metrics
tracing
Spring integration
persistence
search
indexing
audit
workflow
Location

Use a generic cache package.

Suggested package:

codex.fundamentum.api.cache

Suggested files:

codex.fundamentum.api.cache.CacheEntry
codex.fundamentum.api.cache.CacheRegion
codex.fundamentum.api.cache.NoOpCacheRegion
codex.fundamentum.api.cache.ConcurrentMapCacheRegion
codex.fundamentum.api.cache.RecordingCacheRegion

If module exports are required, update module-info.java.

Keep this foundation framework-agnostic.

Do not place cache abstractions in codex.codex.internal if they are intended to be reused by other modules.

1. Create CacheEntry

Create:

codex.fundamentum.api.cache.CacheEntry

Use a sealed interface if consistent with current Java version and project style.

Suggested shape:

public sealed interface CacheEntry<V> permits CacheEntry.Found, CacheEntry.NotFound

Implement two record types:

Found<V>(V value)
NotFound<V>()

Requirements:

Found.value must not be null
NotFound represents a cached negative lookup / 404
add static factories if desired:
CacheEntry.found(value)
CacheEntry.notFound()
add helper methods if useful:
boolean isFound()
boolean isNotFound()
do not store exception state
do not store TTL state
do not store metadata yet

Semantic meaning:

Optional.empty()
-> cache miss

Optional.of(CacheEntry.Found(value))
-> cache hit with value

Optional.of(CacheEntry.NotFound())
-> cache hit for known not-found / 404
2. Create CacheRegion

Create:

codex.fundamentum.api.cache.CacheRegion

Interface shape:

CacheRegion<K, V>

Required methods:

Optional<CacheEntry<V>> get(K key)

CacheEntry<V> getOrLoad(K key, Supplier<? extends CacheEntry<V>> loader)

void put(K key, CacheEntry<V> entry)

void evict(K key)

void clear()

Requirements:

get(...) returns Optional.empty() only when the key is not present in the cache
get(...) returns CacheEntry.NotFound when the negative lookup is cached
getOrLoad(...) loads and stores an entry when absent
getOrLoad(...) must reject null keys
getOrLoad(...) must reject null loader
getOrLoad(...) must reject null loader result
put(...) rejects null key
put(...) rejects null entry
evict(...) rejects null key
clear(...) clears the region
no checked exceptions
no TTL yet
no async API yet

Document the concurrency expectation:

Implementations should avoid executing the loader multiple times for the same key when they can provide atomic loading.
ConcurrentMapCacheRegion should use computeIfAbsent.
CaffeineCacheRegion will later map this to Caffeine cache.get(key, mappingFunction).
Distributed Redis implementations may not guarantee single-flight loading without additional locking.
3. Create NoOpCacheRegion

Create:

codex.fundamentum.api.cache.NoOpCacheRegion

Requirements:

final class
implements CacheRegion<K, V>
get(...) always returns Optional.empty()
getOrLoad(...) validates key and loader, calls loader, validates loader result, returns it, but does not store it
put(...) validates key and entry, does nothing
evict(...) validates key, does nothing
clear(...) does nothing
no external dependencies

This is useful when cache is disabled but callers still use the cache abstraction.

4. Create ConcurrentMapCacheRegion

Create:

codex.fundamentum.api.cache.ConcurrentMapCacheRegion

Requirements:

final class
implements CacheRegion<K, V>
backed by ConcurrentHashMap<K, CacheEntry<V>>
get(...) uses direct map lookup
getOrLoad(...) uses computeIfAbsent(...)
put(...) stores the entry
evict(...) removes the key
clear(...) clears the map
validate null keys
validate null entries
validate null loader
validate null loader result
no TTL
no eviction policy
no maximum size
no metrics
no async behavior

Important:

computeIfAbsent should preserve the negative caching behavior.

If the loader returns CacheEntry.NotFound, that value should be stored like any other entry.

5. Create RecordingCacheRegion

Create:

codex.fundamentum.api.cache.RecordingCacheRegion

Purpose:

test helper
records cache operations
optionally delegates to an internal ConcurrentMapCacheRegion or its own map

Requirements:

final class
implements CacheRegion<K, V>
records operation counts or operation names
records:
get keys
getOrLoad keys
put keys
evict keys
clear count
exposes immutable snapshots:
List<K> getKeys()
List<K> loadKeys()
List<K> putKeys()
List<K> evictKeys()
int clearCount()
supports normal cache behavior, preferably by delegating to ConcurrentMapCacheRegion
has clearRecording() method to clear only recorded operations
has clear() method from CacheRegion that clears cache contents and records clear
snapshots must not expose mutable internals
preserve insertion order for recorded operations
validate nulls consistently

If this feels too large, keep the implementation simple but useful for future service decorator tests.

6. Negative Cache Behavior

Add tests proving negative cache behavior.

Required behavior:

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
-> next get returns Optional.empty()

This is the foundation for 404 caching.

7. No TTL Yet

Do not implement TTL in this task.

However, document future TTL direction:

Found entries may eventually have longer TTL.
NotFound entries may eventually have shorter TTL.

Example future policy:

Found     -> 5 min / 30 min / configurable
NotFound  -> 30 sec / 1 min / configurable

Do not create CachePolicy, CacheConfiguration, or Duration fields yet.

8. No Eager Cache Yet

Do not implement eager refresh / refresh-ahead in this task.

However, document future eager cache direction.

Lazy cache:

request arrives
-> cache lookup
-> load only if missing

Eager / refresh-ahead cache:

registered expensive key/query
-> periodically refresh from canonical source
-> keep value warm before request arrives

Future candidates:

GraphQL result cache
navigation tree cache
menus
site configuration
content type metadata
rendered fragments
popular content lists

Future abstractions may include:

CacheRefreshPolicy
CacheRefresher
EagerCacheRegion
RefreshAheadCacheRegion
scheduled warmers
Caffeine refreshAfterWrite support

Do not implement those now.

9. No Configuration Yet

Codex does not yet have a configuration subsystem.

Do not introduce one in this task.

Keep the cache foundation configurable later but simple now.

Do not add YAML, properties files, env vars, Spring configuration, or runtime configuration objects.

10. Exceptions

All custom Codex/cache exceptions, if any are introduced, should be unchecked.

Prefer NullPointerException via Objects.requireNonNull for null contract violations.

Prefer IllegalArgumentException for invalid arguments.

Do not introduce checked exceptions.

11. Tests

Add plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

CacheEntry tests

Add tests for:

Found rejects null value
Found stores value
NotFound can be created
static factories work if implemented
helper methods work if implemented
NoOpCacheRegion tests

Add tests for:

get returns empty
getOrLoad calls loader every time
getOrLoad returns loader result
getOrLoad rejects null key
getOrLoad rejects null loader
getOrLoad rejects null loader result
put rejects null key
put rejects null entry
evict rejects null key
clear does not fail
ConcurrentMapCacheRegion tests

Add tests for:

get returns empty when key missing
put then get returns found entry
put supports NotFound
getOrLoad loads and stores found entry
getOrLoad loads and stores not-found entry
getOrLoad does not call loader again when found entry exists
getOrLoad does not call loader again when not-found entry exists
evict removes entry
clear removes all entries
null key rejected for get
null key rejected for put
null entry rejected for put
null key rejected for getOrLoad
null loader rejected for getOrLoad
null loader result rejected for getOrLoad
null key rejected for evict

Optional concurrency test:

multiple concurrent getOrLoad calls for same key should call loader once or as close as the implementation guarantees
keep this test simple and non-flaky if implemented
RecordingCacheRegion tests

Add tests for:

records get keys
records getOrLoad keys
records put keys
records evict keys
records clear count
snapshots are immutable
clearRecording clears recorded operations but not necessarily cache contents
clear clears cache contents and records clear
normal cache behavior still works
negative cache behavior still works if it delegates to ConcurrentMapCacheRegion
12. Documentation

Add or update a short note explaining:

cache is not canonical storage
cache accelerates identity reads and repeated misses
CacheEntry.NotFound supports 404 caching
getOrLoad exists to support compute-if-absent style loading
implementations should provide atomic loading when possible
TTL is future work
eager refresh is future work
Caffeine, Redis, and Chronicle Map are future adapters
cache invalidation subscribers are future work

If ADR-007 exists, optionally update it to mention:

Cache foundation now models positive and negative cache entries.
13. Post-Task Report

After implementation, report:

files changed
tests added or updated
Maven command run
whether tests passed
any intentional deviations
any open questions
any recommended follow-up tasks
14. Constraints
    Follow CLAUDE.md.
    Follow CODING_IDENTITY.md.
    Follow AGENT-CALIBRATION.md.
    Keep cache framework-agnostic.
    Do not add Caffeine yet.
    Do not add Redis yet.
    Do not add Chronicle Map yet.
    Do not add TTL yet.
    Do not add eager refresh yet.
    Do not add cache invalidation subscribers yet.
    Do not add cache decorators yet.
    Do not add configuration subsystem.
    Do not add Spring.
    Do not add JPA.
    Do not add persistence framework.
    Do not add REST.
    Do not add search.
    Do not add indexing changes.
    Do not add audit.
    Do not add workflow.
    Do not modify unrelated files.
    Do not modify .idea, target, build, or generated files.
    Keep comments and JavaDoc in English.
    Prefer small, explicit, testable classes.
    Run the smallest relevant Maven test command after implementation.