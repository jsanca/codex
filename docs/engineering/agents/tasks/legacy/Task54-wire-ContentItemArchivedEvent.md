Implement the ContentItem archive lifecycle operation and wire it to ContentItemArchivedEvent dispatching.

Context:
We now have runtime wiring for all ContentItem cache invalidation subscribers.

The following operations already exist and dispatch events:
- create(...) dispatches ContentItemCreatedEvent
- update(...) dispatches ContentItemUpdatedEvent
- publish(...) dispatches ContentItemPublishedEvent
- unpublish(...) dispatches ContentItemUnpublishedEvent

The ContentItemArchivedCacheInvalidationSubscriber is already registered, but ContentItemService does not yet expose an archive operation and EventPublishingContentItemService does not dispatch ContentItemArchivedEvent.

Goal:
Add a semantic archive operation to ContentItemService and ensure ContentItemArchivedEvent is dispatched when it succeeds.

Requirements:
1. Add a command object if consistent with the current style, for example:
    - ArchiveContentItemCommand
2. Add a semantic method to ContentItemService, for example:
    - archive(ArchiveContentItemCommand command, Actor actor)
3. Add the corresponding forwarding method to ForwardingContentItemService.
4. Implement the operation in CodexContentItemService.
5. The operation should transition the ContentItem into an archived state according to the existing ContentItem model.
6. Decide the valid source states based on the current model:
    - DRAFT → ARCHIVED should probably be valid.
    - PUBLISHED → ARCHIVED may require clearing currentPublishedRevisionId or otherwise making the item no longer publicly live.
    - ARCHIVED → ARCHIVED should either be idempotent or rejected consistently with the service’s current lifecycle style.
7. If archiving a published item, make sure the publication state is handled explicitly and not left ambiguous.
8. Update audit fields such as updatedBy/updatedAt if consistent with update/publish/unpublish.
9. EventPublishingContentItemService must dispatch ContentItemArchivedEvent only after the delegate operation succeeds.
10. Do not dispatch the event if the delegate throws.
11. Add a domain exception if invalid archive transitions are possible, following the style of InvalidContentItemUnpublishException.
12. Add focused unit tests for EventPublishingContentItemService proving:
- ContentItemArchivedEvent is dispatched on successful archive.
- No event is dispatched when the delegate fails, if this pattern exists in current tests.
13. Add or update runtime integration tests proving:
- a cached ContentItem identity-read entry is evicted when the item is archived.
- the reloaded ContentItem reflects ARCHIVED state.
14. Update any fake/stub ContentItemService implementations used by existing tests.
15. Follow the same package, naming, command, event, exception, and testing conventions used by:
- UnpublishContentItemCommand
- InvalidContentItemUnpublishException
- ContentItemUnpublishedEvent
- ContentItemUnpublishedCacheInvalidationSubscriber

Constraints:
- Do not implement restore or delete in this task.
- Do not add TTL, cache metrics, cache administration APIs, or new event infrastructure.
- Do not change CachingContentItemService unless strictly necessary.
- Keep the operation semantic; avoid generic CRUD-style behavior.

Expected result:
- ContentItemService exposes an archive lifecycle operation.
- CodexContentItemService implements archive.
- EventPublishingContentItemService dispatches ContentItemArchivedEvent.
- The already-registered ContentItemArchivedCacheInvalidationSubscriber invalidates the cache end-to-end.
- All tests pass.