Create ContentItemArchivedCacheInvalidationSubscriber and ContentItemRestoredCacheInvalidationSubscriber with focused unit tests.

Context:
We already have cache invalidation subscribers for several ContentItem lifecycle events.

ContentItem identity reads are cached using:

ContentItemCacheKey(siteKey, contentTypeKey, contentItemKey)

Goal:
Add cache invalidation for archive and restore lifecycle transitions.

Requirements:
1. Create ContentItemArchivedEvent if it does not already exist.
2. Create ContentItemRestoredEvent if it does not already exist.
3. Each event must expose:
    - SiteKey
    - ContentTypeKey
    - ContentItemKey
4. Create:
    - ContentItemArchivedCacheInvalidationSubscriber
    - ContentItemRestoredCacheInvalidationSubscriber
5. Each subscriber must invalidate the corresponding ContentItemCacheKey.
6. Add focused unit tests for both subscribers:
    - correct cache key invalidated
    - constructor null checks
    - event null checks, if consistent with current subscriber style
7. Follow the same package, naming, and testing conventions used by the existing ContentItem cache invalidation subscribers.
8. Do not add TTL, metrics, cache administration, runtime wiring, or broader event infrastructure in this task.
9. Do not modify CachingContentItemService unless strictly necessary.

Expected result:
- ContentItemArchivedEvent exists.
- ContentItemRestoredEvent exists.
- Both subscribers exist.
- Unit tests pass.