Implement ContentItemUpdatedEvent publishing in EventPublishingContentItemService.

Context:
The runtime now wires all ContentItem cache invalidation subscribers, including ContentItemUpdatedCacheInvalidationSubscriber.

However, EventPublishingContentItemService currently only dispatches:
- ContentItemCreatedEvent
- ContentItemPublishedEvent

Goal:
When an existing ContentItem is updated through ContentItemService, EventPublishingContentItemService should publish ContentItemUpdatedEvent after the delegate operation succeeds.

Requirements:
1. Inspect the current ContentItemService API and identify the existing update-like method.
2. If there is already a semantic update method, wrap it in EventPublishingContentItemService and dispatch ContentItemUpdatedEvent.
3. Build the event using:
    - SiteKey
    - ContentTypeKey
    - ContentItemKey
    - actor/context fields, only if consistent with existing ContentItemCreatedEvent / ContentItemPublishedEvent.
4. Dispatch the event only after the delegate operation succeeds.
5. Do not dispatch the event if the delegate throws.
6. Preserve the existing transaction-aware deferred dispatching behavior.
7. Add or update tests proving:
    - ContentItemUpdatedEvent is recorded/dispatched when an item is updated.
    - The cached identity-read entry is evicted after update.
    - No event is dispatched if the delegate fails, if there is an existing test pattern for this.
8. Do not implement unpublished/deleted/archived/restored in this task.
9. Do not add TTL, cache metrics, or cache administration APIs.

Expected result:
- ContentItemUpdatedEvent is actually published by EventPublishingContentItemService.
- ContentItemUpdatedCacheInvalidationSubscriber receives the event through runtime wiring.
- End-to-end cache invalidation on update is covered by tests.