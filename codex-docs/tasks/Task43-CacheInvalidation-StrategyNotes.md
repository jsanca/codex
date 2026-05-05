# Task43: Cache Invalidation Strategy Notes

## Objective

Document the initial cache invalidation strategy for Codex before implementing cache decorators or cache invalidation subscribers.

This is a documentation/design task.

Do not implement caching services yet.

Do not implement cache invalidation subscribers yet.

Do not wire Caffeine into domain services yet.

The goal is to clarify what can be cached safely, what must be invalidated, and which domain events should eventually drive invalidation.

## Decision Context

Codex now has a cache foundation:

```text
CacheEntry<V>
CacheRegion<K, V>
NoOpCacheRegion<K, V>
ConcurrentMapCacheRegion<K, V>
RecordingCacheRegion<K, V>
CaffeineCacheRegion<K, V>
````

Before adding `CachingContentItemService` or cache invalidation subscribers, we need to define the caching model.

The key distinction is:

```text
Immutable snapshots
  -> safe to cache by id with little or no invalidation

Mutable aggregates / pointers
  -> require event-driven invalidation
```

Examples:

```text
ContentTypeVersion
  -> should behave like an immutable schema snapshot once activated/published

ContentRevision
  -> should behave like an immutable content snapshot

ContentType
  -> mutable aggregate/pointer to current version/draft state

ContentItem
  -> mutable aggregate/pointer to published/working revision state
```

Therefore, cache invalidation should focus primarily on mutable identities and pointers, not immutable snapshots.

## Scope

Create or update documentation.

Suggested file:

```text
codex-docs/cache/CACHE-INVALIDATION-STRATEGY.md
```

If a different documentation convention exists, follow it.

Do not implement:

* `CachingContentItemService`
* `CachingContentTypeService`
* cache invalidation subscribers
* cache key classes
* cache runtime wiring
* TTL policy classes
* eager refresh
* Redis
* Chronicle Map
* Spring Cache
* metrics/tracing
* configuration subsystem

## Required Sections

The document should include:

1. Cache philosophy
2. Three-state cache model
3. Immutable snapshots vs mutable pointers
4. Content type/version invalidation strategy
5. Content item/revision invalidation strategy
6. Negative / 404 cache invalidation
7. Event-to-cache invalidation matrix
8. Near-future implementation plan
9. Future-forward topics
10. Open questions

## 1. Cache Philosophy

Document:

```text
Cache is not canonical storage.

Canonical services and repositories remain the source of truth.

Cache accelerates reads and repeated misses.

Cache invalidation should be driven by domain events.

Cache should not change domain semantics.
```

Mention that cache will usually sit in decorators or projection/read sources, not inside canonical domain logic.

Suggested future shape:

```text
CachingContentItemService
  -> CodexContentItemService

or

CachedContentItemProjectionReader
  -> ContentItemProjectionReader
```

Do not implement these in this task.

## 2. Three-State Cache Model

Document the existing model:

```text
Optional.empty()
  -> real cache miss

Optional.of(CacheEntry.Found(value))
  -> positive cache hit

Optional.of(CacheEntry.NotFound())
  -> negative cache hit / cached 404
```

Explain why `CacheEntry.NotFound` exists:

```text
Repeated requests for missing resources should not repeatedly hit canonical storage.
When the resource is later created, event-driven invalidation removes the NotFound entry.
```

## 3. Immutable Snapshots vs Mutable Pointers

Add a section explaining the core rule:

```text
Immutable snapshots can be cached by id.
Mutable aggregates and mutable pointers require invalidation.
```

Examples of snapshot-like objects:

```text
ContentTypeVersionId -> ContentTypeVersion
ContentRevisionId -> ContentRevision
```

Examples of mutable aggregate/pointer objects:

```text
SiteKey -> Site
SiteId -> Site
ContentTypeKey -> ContentType
ContentItemKey -> ContentItem
latest active content type version pointer
current published content revision pointer
current working content revision pointer
```

Important rule:

```text
Do not invalidate immutable snapshots unless the system explicitly supports mutation of those snapshots.
Instead, invalidate the pointers that decide which snapshot is current.
```

## 4. ContentType / ContentTypeVersion Strategy

Document intended behavior:

```text
ContentTypeVersion should be treated as immutable after activation/publication.

When a schema changes:
  -> create or update draft schema
  -> activation creates a new ContentTypeVersion
  -> ContentType pointer changes to current version
```

Therefore:

### Safe/mostly safe cache

```text
ContentTypeVersionId -> ContentTypeVersion
```

Potentially long TTL or stable cache.

### Requires invalidation

```text
ContentType by siteKey + contentTypeKey
latest active version pointer by siteKey + contentTypeKey
draft schema pointer/cache if introduced
```

### Events that should invalidate ContentType-related cache

Use actual event names if they exist. If exact names differ, document conceptually.

Possible events:

```text
ContentTypeCreatedEvent
ContentTypeFieldAddedEvent or schema draft updated event if exists/future
ContentTypeActivatedEvent
ContentTypeArchivedEvent future
ContentTypeDeletedEvent future
```

Expected invalidation:

```text
ContentTypeCreatedEvent
  -> evict negative cache for content type by key
  -> possibly put or let next read load ContentType

ContentTypeActivatedEvent
  -> evict ContentType by key/id
  -> evict latest active version pointer
  -> do not evict old ContentTypeVersion by id unless versions are mutable

ContentTypeArchivedEvent
  -> evict ContentType by key/id
  -> evict latest active version pointer
```

## 5. ContentItem / ContentRevision Strategy

Document intended behavior:

```text
ContentRevision should be treated as an immutable snapshot.

Publishing does not mutate the revision.
Publishing changes the ContentItem pointer to the published revision.
```

Therefore:

### Safe/mostly safe cache

```text
ContentRevisionId -> ContentRevision
```

Potentially long TTL or stable cache.

### Requires invalidation

```text
ContentItem by siteKey + contentTypeKey + contentItemKey
ContentItem by id
current published revision pointer
current working revision pointer
rendered/page cache future
search/index projections future
```

### Events that should invalidate ContentItem-related cache

Possible events:

```text
ContentItemCreatedEvent
ContentItemPublishedEvent
ContentItemArchivedEvent future
ContentItemUnpublishedEvent future
ContentItemDeletedEvent future
ContentRevisionCreatedEvent future
```

Expected invalidation:

```text
ContentItemCreatedEvent
  -> evict negative cache for content item by key
  -> evict content item by id/key if present

ContentItemPublishedEvent
  -> evict ContentItem by key/id
  -> evict current published revision pointer
  -> do not evict ContentRevision by id if revisions are immutable
  -> future: evict rendered fragments/pages
  -> future: indexing/search projection already handled by index subscriber

ContentItemArchivedEvent
  -> evict ContentItem by key/id
  -> evict current published revision pointer
  -> future: evict rendered fragments/pages

ContentItemDeletedEvent
  -> evict ContentItem by key/id
  -> optionally store NotFound after next miss
```

## 6. Negative / 404 Cache Invalidation

Document the 404 strategy explicitly.

Example:

```text
1. Request content item /blog/missing.
2. Cache misses.
3. Canonical read returns not found.
4. Cache stores CacheEntry.NotFound.
5. Repeated requests return cached NotFound.
6. Later another node creates /blog/missing.
7. ContentItemCreatedEvent evicts the key.
8. Next request loads CacheEntry.Found(value).
```

Important:

```text
Create/update/publish events should evict both positive entries and negative entries for the affected key.
```

Do not implement distributed invalidation yet.

Mention future distributed concern:

```text
In a cluster, invalidation events must reach all nodes or be propagated through a shared cache/message system.
```

## 7. Event-to-Cache Invalidation Matrix

Add a practical matrix.

Suggested columns:

```text
Event | Cache keys affected | Positive entries | Negative entries | Notes
```

Include at least:

```text
SiteCreatedEvent
ContentTypeCreatedEvent
ContentTypeActivatedEvent
ContentItemCreatedEvent
ContentItemPublishedEvent
```

Example rows:

```text
SiteCreatedEvent
  Keys:
    site by siteKey
    site by id
  Positive:
    evict or refresh
  Negative:
    evict site by siteKey NotFound
  Notes:
    New site may satisfy previous misses.

ContentTypeActivatedEvent
  Keys:
    content type by key/id
    latest active version pointer
  Positive:
    evict
  Negative:
    usually not relevant unless previous lookup missed
  Notes:
    Do not evict immutable versions by id.

ContentItemPublishedEvent
  Keys:
    content item by key/id
    published revision pointer
    rendered content future
  Positive:
    evict
  Negative:
    evict by key if present
  Notes:
    Do not evict immutable revision by id.
```

## 8. Near-Future Implementation Plan

Document a suggested implementation sequence.

Recommended:

```text
Task44: ContentItem cache keys and CachingContentItemService for identity reads

Task45: ContentItem cache invalidation subscriber

Task46: ContentType cache keys and CachingContentTypeService for identity reads

Task47: ContentType cache invalidation subscriber

Task48: ContentRevision / ContentTypeVersion snapshot cache discussion or implementation
```

Mention that `ContentItem` should probably be first because publish changes pointers and is already covered by events.

## 9. Future-Forward Topics

Document but do not implement:

```text
TTL policies
Different TTL for Found vs NotFound
Caffeine expireAfterWrite
Caffeine refreshAfterWrite
Eager / refresh-ahead cache
Redis shared cache
Chronicle Map local persistent cache
Distributed invalidation
Cache metrics
Cache tracing
Cache warmers
Cache region naming
Configuration subsystem
```

Include the future TTL idea:

```text
Found     -> longer TTL
NotFound  -> shorter TTL
```

But state:

```text
No TTL policy exists yet.
```

## 10. Open Questions

Include open questions such as:

```text
Should ContentTypeVersion and ContentRevision be formally declared immutable after creation/activation?

Should snapshot caches use very long TTL, no TTL, or event-based eviction only?

Should negative cache entries have a shorter TTL once configuration exists?

Should cache invalidation subscribers live in codex-codex, codex-concilium, or a future cache module?

Should cache decorators sit around services, projection readers, or both?

How should distributed invalidation work once Redis or cluster coordination exists?

Should cache keys be records in api packages or internal cache packages?
```

Do not resolve these in code.

## Documentation Update

If `MODULE-RESPONSIBILITIES.md` has a cache/fundamentum section, optionally add a small note:

```text
Cache invalidation strategy is documented in codex-docs/cache/CACHE-INVALIDATION-STRATEGY.md.
```

Do not rewrite large docs.

## Post-Task Report

After completion, report:

* files created
* files modified
* documentation sections added
* any terminology decisions
* any open questions discovered
* recommended next task

## Constraints

* Documentation-only task.
* Do not write Java code.
* Do not modify production code.
* Do not modify tests.
* Do not modify Maven dependencies.
* Do not modify module-info.java.
* Do not implement cache decorators.
* Do not implement invalidation subscribers.
* Do not implement TTL.
* Do not implement refresh-ahead.
* Do not implement Redis.
* Do not implement Chronicle Map.
* Do not introduce Spring.
* Keep documentation in English.
* Keep the strategy practical and implementation-oriented.

