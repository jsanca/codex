Wire the existing ContentItem cache invalidation subscribers into the Codex runtime.

Context:
We already have the following subscribers:

- ContentItemCreatedCacheInvalidationSubscriber
- ContentItemUpdatedCacheInvalidationSubscriber
- ContentItemPublishedCacheInvalidationSubscriber
- ContentItemUnpublishedCacheInvalidationSubscriber
- ContentItemArchivedCacheInvalidationSubscriber
- ContentItemRestoredCacheInvalidationSubscriber
- ContentItemDeletedCacheInvalidationSubscriber

They all invalidate ContentItemCacheKey entries from the ContentItem identity-read cache.

Goal:
Register these subscribers in the runtime so they are available to the CodexEventDispatcher.

Requirements:
1. Find the current runtime/module wiring pattern used for existing subscribers such as:
    - ContentItemPublishedChroniconSubscriber
    - ContentItemPublishedIndexingSubscriber
    - ContentTypeCreatedChroniconSubscriber
    - SiteCreatedChroniconSubscriber
2. Follow the same existing registration style.
3. Wire all ContentItem cache invalidation subscribers.
4. Ensure they share/use the same CacheRegion<ContentItemCacheKey, ContentItem> used by CachingContentItemService.
5. Do not introduce TTL, cache metrics, cache administration APIs, or new event infrastructure.
6. Do not change subscriber behavior unless required by wiring.
7. Add or update tests only if there is an existing runtime wiring test pattern.
8. Keep the task focused on registration/wiring.

Expected result:
- All ContentItem cache invalidation subscribers are registered in the runtime.
- Existing tests pass.
- Any relevant runtime wiring tests are updated.