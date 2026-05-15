Create ContentItemUnpublishedCacheInvalidationSubscriber with focused unit tests.

Context:
We already have:
- CachingContentItemService
- ContentItemPublishedCacheInvalidationSubscriber
- ContentItemUpdatedCacheInvalidationSubscriber

ContentItem identity reads are cached using:

ContentItemCacheKey(siteKey, contentTypeKey, contentItemKey)

Goal:
Add the equivalent cache invalidation subscriber for ContentItemUnpublished events.

Requirements:
1. Create ContentItemUnpublishedEvent if it does not already exist.
2. The event must expose enough information to build a ContentItemCacheKey:
    - SiteKey
    - ContentTypeKey
    - ContentItemKey
3. Create ContentItemUnpublishedCacheInvalidationSubscriber.
4. The subscriber must invalidate the corresponding ContentItemCacheKey from the cache region.
5. Add focused unit tests verifying:
    - the correct cache key is invalidated
    - constructor null checks
    - event null checks, if consistent with the existing subscriber style
6. Follow the same package, naming, style, and conventions used by ContentItemPublishedCacheInvalidationSubscriber and ContentItemUpdatedCacheInvalidationSubscriber.
7. Do not add TTL, cache metrics, cache administration, runtime wiring, or broader event infrastructure in this task.
8. Do not modify CachingContentItemService unless strictly necessary.

Expected result:
- ContentItemUnpublishedEvent exists if missing.
- ContentItemUnpublishedCacheInvalidationSubscriber exists.
- Unit tests pass.