# Task45: ContentItem Cache Invalidation Subscribers

## Objective

Introduce event-driven cache invalidation for `ContentItem` identity reads.

Task44 introduced:

- `ContentItemCacheKey`
- `CachingContentItemService`

The decorator caches:

- positive results using `CacheEntry.Found(ContentItem)`
- negative / 404 results using `CacheEntry.NotFound()`

This task adds subscribers that evict affected `ContentItem` cache entries when domain events indicate that the cached item or cached miss may be stale.

Do not wire `CachingContentItemService` into `CodexRuntime` yet.

Do not wire cache into `ConciliumRuntime` yet.

Do not add TTL, metrics, admin APIs, or cache configuration.

## Decision Context

`ContentItem` is a mutable aggregate/pointer.

Publishing a content item may change its current published revision pointer.

Creating a content item may turn a previously cached `NotFound` into a valid item.

Therefore, cache invalidation should be driven by domain events.

Expected behavior:

```text
ContentItemCreatedEvent
  -> evict ContentItemCacheKey(siteKey, contentTypeKey, contentItemKey)

ContentItemPublishedEvent
  -> evict ContentItemCacheKey(siteKey, contentTypeKey, contentItemKey)
````

This invalidates both:

* stale positive cache entries
* stale negative / 404 cache entries

The decorator remains read-only. Mutating operations should not manually evict inside `CachingContentItemService`.

## Scope

Implement:

* one or more cache invalidation subscribers for `ContentItem`
* tests
* optional small internal helper if duplication is obvious

Do not implement:

* runtime wiring
* Caffeine runtime configuration
* TTL
* refresh-ahead
* Redis
* Chronicle Map
* distributed invalidation
* cache metrics
* cache admin endpoints
* cache management API
* ContentType cache invalidation
* ContentRevision cache invalidation
* Site cache invalidation
* search/index invalidation changes
* audit/Chronicon changes
* workflow changes

## Package Location

Suggested package:

```text
codex.codex.internal.cache
```

Create classes such as:

```text
ContentItemCreatedCacheInvalidationSubscriber
ContentItemPublishedCacheInvalidationSubscriber
```

or, if simpler and clean:

```text
ContentItemCacheInvalidationSubscriber
```

that handles both event types only if the existing `CodexEventSubscriber` type model supports this cleanly.

Important:

`CodexEventSubscriber<E>` likely has one `eventType()` method, so separate subscribers may be clearer.

Prefer separate small subscribers if generic multi-event handling would complicate the design.

## 1. ContentItemCreatedEvent invalidation

Create subscriber:

```text
ContentItemCreatedCacheInvalidationSubscriber
```

It should implement:

```text
CodexEventSubscriber<ContentItemCreatedEvent>
```

Behavior:

```text
handle(event)
  -> validate event non-null
  -> build ContentItemCacheKey(event.siteKey(), event.contentTypeKey(), event.key())
  -> cache.evict(cacheKey)
```

Requirements:

* constructor receives `CacheRegion<ContentItemCacheKey, ContentItem>`
* constructor validates cache non-null
* `eventType()` returns `ContentItemCreatedEvent.class`
* `handle(...)` rejects null event
* no logging required, debug logging acceptable
* no runtime wiring
* no other cache keys

Purpose:

A create event should evict a possible previous `NotFound` entry for that content item key.

## 2. ContentItemPublishedEvent invalidation

Create subscriber:

```text
ContentItemPublishedCacheInvalidationSubscriber
```

It should implement:

```text
CodexEventSubscriber<ContentItemPublishedEvent>
```

Behavior:

```text
handle(event)
  -> validate event non-null
  -> build ContentItemCacheKey(event.siteKey(), event.contentTypeKey(), event.key())
  -> cache.evict(cacheKey)
```

Requirements:

* constructor receives `CacheRegion<ContentItemCacheKey, ContentItem>`
* constructor validates cache non-null
* `eventType()` returns `ContentItemPublishedEvent.class`
* `handle(...)` rejects null event
* no runtime wiring
* no other cache keys

Purpose:

A publish event may change the current published revision pointer on the `ContentItem`, so the cached `ContentItem` must be evicted.

## 3. Do Not Evict Snapshot Caches

Do not invalidate:

```text
ContentRevisionId -> ContentRevision
ContentTypeVersionId -> ContentTypeVersion
```

Those are snapshot-like objects and are not part of this task.

This task only invalidates:

```text
ContentItemCacheKey -> ContentItem
```

## 4. Tests

Add JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Use:

```text
RecordingCacheRegion<ContentItemCacheKey, ContentItem>
```

or:

```text
ConcurrentMapCacheRegion<ContentItemCacheKey, ContentItem>
```

Recommended test file(s):

```text
codex-codex/src/test/java/codex/codex/internal/cache/ContentItemCreatedCacheInvalidationSubscriberTest.java
codex-codex/src/test/java/codex/codex/internal/cache/ContentItemPublishedCacheInvalidationSubscriberTest.java
```

or one combined test class if that is cleaner.

### ContentItemCreatedCacheInvalidationSubscriber tests

Test cases:

* constructor rejects null cache
* `eventType()` returns `ContentItemCreatedEvent.class`
* `handle` rejects null event
* `handle` evicts expected cache key
* evicts existing positive entry
* evicts existing negative `NotFound` entry

### ContentItemPublishedCacheInvalidationSubscriber tests

Test cases:

* constructor rejects null cache
* `eventType()` returns `ContentItemPublishedEvent.class`
* `handle` rejects null event
* `handle` evicts expected cache key
* evicts existing positive entry
* evicts existing negative `NotFound` entry

## 5. Optional integration test

Add a small integration-style test only if simple.

Possible flow:

```text
CachingContentItemService caches NotFound
ContentItemCreatedCacheInvalidationSubscriber receives ContentItemCreatedEvent
cache entry is evicted
next findByKey calls delegate again
```

This can prove negative cache invalidation.

Do not overbuild.

Do not wire through `CodexRuntime`.

Manual subscriber invocation is enough.

## 6. Documentation

Update `codex-docs/cache/CACHE-INVALIDATION-STRATEGY.md` if useful.

Add a small note:

```text
ContentItemCreatedEvent and ContentItemPublishedEvent now have cache invalidation subscribers for ContentItem identity reads.
```

Do not rewrite the document.

## 7. Acceptance Criteria

Task is complete when:

* content item create invalidation subscriber exists
* content item publish invalidation subscriber exists
* subscribers implement `CodexEventSubscriber`
* subscribers evict `ContentItemCacheKey`
* positive entries are evicted
* negative / NotFound entries are evicted
* no runtime wiring is introduced
* no TTL/config/admin/metrics are introduced
* tests pass

## 8. Maven Commands

Run:

```bash
mvn test -pl codex-codex -am
```

If practical, also run:

```bash
mvn compile
```

Report command results.

## 9. Post-Task Report

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

## 10. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Keep invalidation event-driven.
* Do not evict inside `CachingContentItemService` mutating methods.
* Do not wire cache into runtime yet.
* Do not introduce TTL.
* Do not introduce cache metrics.
* Do not introduce cache admin APIs.
* Do not introduce configuration.
* Do not introduce Redis.
* Do not introduce Chronicle Map.
* Do not introduce distributed invalidation.
* Do not change `CacheRegion`.
* Do not change `CacheEntry`.
* Do not change service APIs.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.

