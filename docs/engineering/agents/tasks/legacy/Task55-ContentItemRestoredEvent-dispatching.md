Implement the ContentItem restore lifecycle operation and wire it to ContentItemRestoredEvent dispatching.

Context:
We now have runtime wiring for all ContentItem cache invalidation subscribers.

The following ContentItem lifecycle operations already exist and dispatch events:
- create(...) dispatches ContentItemCreatedEvent
- update(...) dispatches ContentItemUpdatedEvent
- publish(...) dispatches ContentItemPublishedEvent
- unpublish(...) dispatches ContentItemUnpublishedEvent
- archive(...) dispatches ContentItemArchivedEvent

The ContentItemRestoredCacheInvalidationSubscriber is already registered, but ContentItemService does not yet expose a restore operation and EventPublishingContentItemService does not dispatch ContentItemRestoredEvent.

Goal:
Add a semantic restore operation to ContentItemService and ensure ContentItemRestoredEvent is dispatched when it succeeds.

Requirements:
1. Add a command object if consistent with the current style, for example:
    - RestoreContentItemCommand
2. Add a semantic method to ContentItemService, for example:
    - restore(RestoreContentItemCommand command, Actor actor)
3. Add the corresponding forwarding method to ForwardingContentItemService.
4. Implement the operation in CodexContentItemService.
5. The operation should transition a ContentItem from ARCHIVED back into an active editable state according to the current model.
6. Suggested initial semantics:
    - ARCHIVED → DRAFT
    - restore should not automatically republish the item
    - currentPublishedRevisionId should remain empty unless the existing model strongly requires otherwise
7. Reject invalid transitions if consistent with current lifecycle style:
    - DRAFT → restore should probably be invalid
    - PUBLISHED → restore should probably be invalid
8. Add a domain exception if invalid restore transitions are possible, following the style of InvalidContentItemUnpublishException and any archive exception.
9. Update audit fields such as updatedBy/updatedAt if consistent with update/publish/unpublish/archive.
10. EventPublishingContentItemService must dispatch ContentItemRestoredEvent only after the delegate operation succeeds.
11. Do not dispatch the event if the delegate throws.
12. Add focused unit tests for EventPublishingContentItemService proving:
- ContentItemRestoredEvent is dispatched on successful restore
- no event is dispatched when the delegate fails, if this pattern exists in current tests
13. Add or update runtime integration tests proving:
- a cached ContentItem identity-read entry is evicted when the item is restored
- the reloaded ContentItem reflects the restored state
14. Update any fake/stub ContentItemService implementations used by existing tests.
15. Follow the same package, naming, command, event, exception, and testing conventions used by:
- ArchiveContentItemCommand
- ContentItemArchivedEvent
- ContentItemArchivedCacheInvalidationSubscriber

Constraints:
- Do not implement delete in this task.
- Do not add TTL, cache metrics, cache administration APIs, or new event infrastructure.
- Do not change CachingContentItemService unless strictly necessary.
- Keep the operation semantic; avoid generic CRUD-style behavior.

Expected result:
- ContentItemService exposes a restore lifecycle operation.
- CodexContentItemService implements restore.
- EventPublishingContentItemService dispatches ContentItemRestoredEvent.
- The already-registered ContentItemRestoredCacheInvalidationSubscriber invalidates the cache end-to-end.
- All tests pass.