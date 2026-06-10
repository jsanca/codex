Create ContentItemUpdatedCacheInvalidationSubscriber with focused unit tests.

Context:
We already have CachingContentItemService, which caches ContentItem identity reads using:

ContentItemCacheKey(siteKey, contentTypeKey, contentItemKey)

We also already have ContentItemPublishedCacheInvalidationSubscriber with tests.

Goal:
Add the equivalent cache invalidation subscriber for ContentItemUpdated events.

Requirements:
1. Create a ContentItemUpdated domain event if it does not already exist.
2. The event must expose enough information to build a ContentItemCacheKey:
    - SiteKey
    - ContentTypeKey
    - ContentItemKey
3. Create ContentItemUpdatedCacheInvalidationSubscriber.
4. The subscriber must invalidate the corresponding ContentItemCacheKey from the cache region.
5. Add focused unit tests verifying:
    - the correct cache key is invalidated
    - constructor null checks
    - event null checks, if consistent with the existing subscriber style
6. Follow the same package, naming, style, and conventions used by ContentItemPublishedCacheInvalidationSubscriber.
7. Do not add TTL, cache metrics, cache administration, runtime wiring, or broader event infrastructure in this task.
8. Do not modify CachingContentItemService unless strictly necessary.

Expected result:
- ContentItemUpdated event exists if missing.
- ContentItemUpdatedCacheInvalidationSubscriber exists.
- Unit tests pass.