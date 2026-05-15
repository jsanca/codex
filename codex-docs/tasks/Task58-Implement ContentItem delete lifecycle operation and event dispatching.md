Implement the ContentItem delete lifecycle operation and wire it to ContentItemDeletedEvent dispatching.

Context:
We now have runtime wiring for all ContentItem cache invalidation subscribers.

The following ContentItem lifecycle operations already exist and dispatch events:
- create(...) dispatches ContentItemCreatedEvent
- update(...) dispatches ContentItemUpdatedEvent
- publish(...) dispatches ContentItemPublishedEvent
- unpublish(...) dispatches ContentItemUnpublishedEvent
- archive(...) dispatches ContentItemArchivedEvent
- restore(...) dispatches ContentItemRestoredEvent

The ContentItemDeletedCacheInvalidationSubscriber is already registered, but ContentItemService does not yet expose a delete operation and EventPublishingContentItemService does not dispatch ContentItemDeletedEvent.

Goal:
Add a semantic delete operation to ContentItemService and ensure ContentItemDeletedEvent is dispatched when it succeeds.

Requirements:
1. Inspect the current ContentItem model and repository capabilities before choosing hard delete vs soft delete.
2. Add a command object if consistent with the current style, for example:
    - DeleteContentItemCommand
3. Add a semantic method to ContentItemService, for example:
    - delete(DeleteContentItemCommand command, Actor actor)
4. Add the corresponding forwarding method to ForwardingContentItemService.
5. Implement the operation in CodexContentItemService according to the current model:
    - If the repository already supports removal, use that.
    - If the model already supports a deleted/tombstone status, use that.
    - If neither exists, stop and report the smallest model/repository addition needed instead of inventing a broad deletion model.
6. Deleting a published item must not leave an ambiguous live/publication state.
7. If using hard delete:
    - after delete, findByKey should return Optional.empty()
    - cached Found entries must be invalidated through ContentItemDeletedEvent
8. If using soft delete:
    - the ContentItem must transition to a clear deleted/tombstone state
    - publication pointers must be cleared if applicable
    - audit fields should be updated consistently
9. Reject invalid transitions if consistent with the current lifecycle style.
10. Add a domain exception if invalid delete transitions are possible, following the style of InvalidContentItemArchiveException / InvalidContentItemRestoreException.
11. EventPublishingContentItemService must dispatch ContentItemDeletedEvent only after the delegate operation succeeds.
12. Do not dispatch the event if the delegate throws.
13. Add focused unit tests for EventPublishingContentItemService proving:
- ContentItemDeletedEvent is dispatched on successful delete
- no event is dispatched when the delegate fails, if this pattern exists in current tests
14. Add or update runtime integration tests proving:
- a cached ContentItem identity-read entry is evicted when the item is deleted
- after deletion, a fresh findByKey reflects the expected absence or deleted state according to the chosen semantics
15. Update any fake/stub ContentItemService implementations used by existing tests.
16. Follow the same package, naming, command, event, exception, and testing conventions used by:
- ArchiveContentItemCommand
- RestoreContentItemCommand
- ContentItemDeletedEvent
- ContentItemDeletedCacheInvalidationSubscriber

Constraints:
- Do not add TTL, cache metrics, cache administration APIs, or new event infrastructure.
- Do not change CachingContentItemService unless strictly necessary.
- Keep the operation semantic; avoid generic CRUD-style repository behavior.
- Do not invent a complex deletion/audit/tombstone model in this task. If the current model does not support deletion cleanly, report that and propose the smallest next step.

Expected result:
- ContentItemService exposes a delete lifecycle operation if the current model supports it.
- CodexContentItemService implements delete or reports the missing model/repository capability.
- EventPublishingContentItemService dispatches ContentItemDeletedEvent.
- The already-registered ContentItemDeletedCacheInvalidationSubscriber invalidates the cache end-to-end.
- All tests pass.