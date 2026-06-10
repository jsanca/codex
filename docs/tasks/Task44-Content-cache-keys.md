# Task44: ContentItem Cache Keys and CachingContentItemService

## Objective

Introduce the first cache-backed service decorator for identity reads of `ContentItem`.

This task should add a cache key and a `CachingContentItemService` decorator that caches `findByKey(...)` lookups using the existing `CacheRegion` abstraction.

The decorator must support both:

- positive cache hits with `CacheEntry.Found(ContentItem)`
- negative / 404 cache hits with `CacheEntry.NotFound()`

Do not wire this decorator into `CodexRuntime` yet.

Do not wire it into `ConciliumRuntime` yet.

Do not implement invalidation subscribers yet.

This task only introduces the decorator and tests it in isolation.

## Decision Context

Codex now has:

- `CacheEntry<V>`
- `CacheRegion<K, V>`
- `CaffeineCacheRegion<K, V>`
- cache invalidation strategy documentation
- `ContentItemService`
- the forwarding/decorator service pattern

The cache invalidation strategy established the distinction:

```text
Immutable snapshots
  -> safe to cache by id

Mutable aggregates / pointers
  -> require event-driven invalidation
````

`ContentItem` is a mutable aggregate/pointer because publishing changes its current published revision pointer.

Therefore, this task must not wire the cache globally yet.

First we add and test the decorator.

A future task will add invalidation subscribers for events such as:

* `ContentItemCreatedEvent`
* `ContentItemPublishedEvent`
* future archive/unpublish/delete events

## Scope

Implement:

* `ContentItemCacheKey`
* `CachingContentItemService`
* unit tests

Do not implement:

* runtime wiring
* cache invalidation subscribers
* Caffeine configuration in runtime
* TTL
* refresh-ahead
* Redis
* Chronicle Map
* distributed invalidation
* ContentType cache
* ContentRevision cache
* ContentTypeVersion cache
* search cache
* rendered page cache

## Package Location

Suggested package:

```text
codex.codex.internal.cache
```

Create:

```text
codex.codex.internal.cache.ContentItemCacheKey
codex.codex.internal.service.CachingContentItemService
```

Alternative package placement is acceptable if it follows current conventions.

The decorator belongs near other service decorators.

The key can live in an internal cache package because it is an implementation detail of the decorator.

Do not expose cache keys as public API yet.

## 1. Create ContentItemCacheKey

Create a Java record:

```java
public record ContentItemCacheKey(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey contentItemKey
) {
}
```

Requirements:

* validate all components non-null
* use existing value objects:

    * `SiteKey`
    * `ContentTypeKey`
    * `ContentItemKey`
* JavaDoc should explain this is the identity-read cache key for content item lookups
* do not use raw strings
* do not add tenant-aware fields yet
* do not add locale/variant fields yet unless already required by the current service signature

Future note:

```text
Locale, variant, tenant/client id, or channel may become part of this key later.
```

Do not implement that now.

## 2. Create CachingContentItemService

Create:

```text
codex.codex.internal.service.CachingContentItemService
```

It should implement the existing forwarding/decorator pattern.

Expected shape:

```java
public final class CachingContentItemService implements ForwardingContentItemService {

    private final ContentItemService delegate;
    private final CacheRegion<ContentItemCacheKey, ContentItem> byKeyCache;

    public CachingContentItemService(
            ContentItemService delegate,
            CacheRegion<ContentItemCacheKey, ContentItem> byKeyCache
    ) {
        ...
    }

    @Override
    public ContentItemService getDelegate() {
        return delegate;
    }

    @Override
    public Optional<ContentItem> findByKey(
            SiteKey siteKey,
            ContentTypeKey contentTypeKey,
            ContentItemKey key
    ) {
        ...
    }
}
```

Use the actual `ContentItemService.findByKey(...)` signature.

If the service method has additional parameters, preserve them.

Do not invent new public methods.

## 3. Cache Behavior

For `findByKey(...)`:

1. validate arguments consistently with existing service style
2. build `ContentItemCacheKey`
3. call `byKeyCache.getOrLoad(...)`
4. delegate lookup on cache miss
5. store:

    * `CacheEntry.Found(item)` when delegate returns item
    * `CacheEntry.NotFound()` when delegate returns empty
6. return:

    * `Optional.of(item)` for `Found`
    * `Optional.empty()` for `NotFound`

Conceptual implementation:

```java
final CacheEntry<ContentItem> entry = byKeyCache.getOrLoad(cacheKey, () ->
        delegate.findByKey(siteKey, contentTypeKey, key)
                .<CacheEntry<ContentItem>>map(CacheEntry::found)
                .orElseGet(CacheEntry::notFound)
);

return switch (entry) {
    case CacheEntry.Found<ContentItem> found -> Optional.of(found.value());
    case CacheEntry.NotFound<ContentItem> ignored -> Optional.empty();
};
```

If pattern matching for switch is not enabled or not consistent with project style, use `instanceof`.

## 4. Mutating Operations

Do not cache mutating operations.

For now, mutating operations should be forwarded through `ForwardingContentItemService` defaults unless a specific override is necessary.

Do not evict cache entries in this decorator yet.

Reason:

Invalidation should be event-driven and handled by a future subscriber.

This avoids mixing mutation semantics and invalidation policy into the read decorator.

## 5. Negative Cache Behavior

The decorator must cache misses.

Expected behavior:

```text
first missing lookup
  -> delegate called
  -> cache stores NotFound
  -> returns Optional.empty()

second same missing lookup
  -> delegate not called
  -> cache returns NotFound
  -> returns Optional.empty()
```

This is important for repeated 404s.

## 6. No Runtime Wiring

Do not add `CachingContentItemService` to:

* `CodexRuntime`
* `ConciliumRuntime`
* tests that rely on global runtime composition

The decorator should be tested in isolation.

Runtime wiring must wait until cache invalidation subscribers exist.

## 7. Tests

Add JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer a hand-written fake/stub delegate service.

Suggested test file:

```text
codex-codex/src/test/java/codex/codex/internal/service/CachingContentItemServiceTest.java
```

### Constructor tests

* constructor rejects null delegate
* constructor rejects null cache region

### findByKey tests

* rejects null siteKey
* rejects null contentTypeKey
* rejects null contentItemKey
* first existing lookup calls delegate and returns item
* second existing lookup returns cached item and does not call delegate again
* first missing lookup calls delegate and returns empty
* second missing lookup returns cached NotFound and does not call delegate again
* cache key uses site key, content type key, and content item key
* different content item keys produce different cache entries
* different content type keys produce different cache entries
* different site keys produce different cache entries

### forwarding behavior tests

If easy, add one test proving mutating operations still forward to the delegate.

Do not overbuild.

## 8. Test Double Guidance

Use a simple fake delegate.

Example behavior:

```java
private static final class RecordingContentItemService implements ContentItemService {
    private final Map<ContentItemCacheKey, ContentItem> items = new HashMap<>();
    private int findByKeyCalls;

    Optional<ContentItem> findByKey(...) {
        findByKeyCalls++;
        return Optional.ofNullable(items.get(...));
    }
}
```

If implementing all `ContentItemService` methods is too verbose, use `ForwardingContentItemService` with a small fake delegate if consistent with existing test style.

Avoid Mockito.

## 9. Cache Implementation in Tests

Use:

```java
ConcurrentMapCacheRegion<ContentItemCacheKey, ContentItem>
```

or:

```java
RecordingCacheRegion<ContentItemCacheKey, ContentItem>
```

Use `RecordingCacheRegion` if it helps assert cache interactions.

Use `ConcurrentMapCacheRegion` if behavior assertions are enough.

Do not use Caffeine in these tests unless there is a specific reason. Caffeine adapter already has its own tests.

## 10. Documentation

Update cache strategy documentation only if useful.

Possible note:

```text
Task44 introduced CachingContentItemService as the first isolated identity-read cache decorator.
It is not wired into runtime until event-driven invalidation exists.
```

Do not rewrite large docs.

## 11. Acceptance Criteria

Task is complete when:

* `ContentItemCacheKey` exists
* `CachingContentItemService` exists
* `findByKey(...)` caches positive results
* `findByKey(...)` caches negative / NotFound results
* repeated positive lookup does not call delegate again
* repeated negative lookup does not call delegate again
* mutating operations are not cached
* decorator is not wired into runtime
* tests pass

## 12. Maven Commands

Run:

```bash
mvn test -pl codex-codex -am
```

If practical, also run:

```bash
mvn compile
```

Report command results.

## 13. Post-Task Report

After implementation, report:

* files created
* files modified
* tests added
* Maven commands run
* whether tests passed
* whether runtime wiring was avoided
* intentional deviations
* open questions
* recommended follow-up tasks

## 14. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Use existing decorator/forwarding style.
* Do not change `ContentItemService` API unless absolutely necessary.
* Do not change `CacheRegion`.
* Do not change `CacheEntry`.
* Do not wire cache into runtime.
* Do not implement invalidation subscribers.
* Do not add TTL.
* Do not add refresh-ahead.
* Do not add Redis.
* Do not add Chronicle Map.
* Do not add Spring.
* Do not add persistence.
* Do not add search.
* Do not add workflow.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.
* Prefer small, explicit, testable code.

