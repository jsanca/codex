Implement the ContentItem unpublish lifecycle operation and wire it to ContentItemUnpublishedEvent dispatching.

Context:
We now have runtime wiring for all ContentItem cache invalidation subscribers.

The following operations already exist and dispatch events:
- create(...) dispatches ContentItemCreatedEvent
- update(...) dispatches ContentItemUpdatedEvent
- publish(...) dispatches ContentItemPublishedEvent

The ContentItemUnpublishedCacheInvalidationSubscriber is already registered, but ContentItemService does not yet expose an unpublish operation and EventPublishingContentItemService does not dispatch ContentItemUnpublishedEvent.

Goal:
Add a semantic unpublish operation to ContentItemService and ensure ContentItemUnpublishedEvent is dispatched when it succeeds.

Requirements:
1. Add a command object if consistent with the current style, for example:
    - UnpublishContentItemCommand
2. Add a semantic method to ContentItemService, for example:
    - unpublish(UnpublishContentItemCommand command, Actor actor)
3. Add the corresponding forwarding method to ForwardingContentItemService.
4. Implement the operation in CodexContentItemService.
5. The operation should clear or update the live/publication state according to the existing ContentItem model.
6. It should update audit fields such as updatedBy/updatedAt if consistent with the update/publish implementation.
7. EventPublishingContentItemService must dispatch ContentItemUnpublishedEvent only after the delegate operation succeeds.
8. Do not dispatch the event if the delegate throws.
9. Add focused unit tests for EventPublishingContentItemService proving:
    - ContentItemUnpublishedEvent is dispatched on successful unpublish.
    - No event is dispatched when the delegate fails, if this pattern exists in current tests.
10. Add or update runtime integration tests proving:
- a cached ContentItem identity-read entry is evicted when the item is unpublished.
11. Update any fake/stub ContentItemService implementations used by existing tests.
12. Follow the same package, naming, command, event, and testing conventions used by:
- UpdateContentItemCommand
- ContentItemUpdatedEvent
- ContentItemUpdatedCacheInvalidationSubscriber
- existing publish operation

Constraints:
- Do not implement delete, archive, or restore in this task.
- Do not add TTL, cache metrics, cache administration APIs, or new event infrastructure.
- Do not change CachingContentItemService unless strictly necessary.
- Keep the operation semantic; avoid introducing generic save/delete CRUD behavior.

Expected result:
- ContentItemService exposes an unpublish lifecycle operation.
- CodexContentItemService implements unpublish.
- EventPublishingContentItemService dispatches ContentItemUnpublishedEvent.
- The already-registered ContentItemUnpublishedCacheInvalidationSubscriber invalidates the cache end-to-end.
- All tests pass.