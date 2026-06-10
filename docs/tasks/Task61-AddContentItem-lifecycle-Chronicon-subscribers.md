Add Chronicon audit subscribers for the remaining non-delete ContentItem lifecycle events.

Context:
ADR-008 documents the current ContentItem lifecycle semantics.

Currently, Chronicon only has:

- `ContentItemPublishedChroniconSubscriber`

The existing published subscriber is wired in ChroniconRuntime and follows the correct pattern:
it relies only on event fields and does not reload the entity from a repository.

Goal:
Add Chronicon subscribers for the remaining non-delete ContentItem lifecycle events:

- ContentItemCreatedEvent
- ContentItemUpdatedEvent
- ContentItemUnpublishedEvent
- ContentItemArchivedEvent
- ContentItemRestoredEvent

Requirements:
1. Create one Chronicon subscriber per event:
    - ContentItemCreatedChroniconSubscriber
    - ContentItemUpdatedChroniconSubscriber
    - ContentItemUnpublishedChroniconSubscriber
    - ContentItemArchivedChroniconSubscriber
    - ContentItemRestoredChroniconSubscriber

2. Follow the style and mapping pattern of:
    - ContentItemPublishedChroniconSubscriber

3. Do not reload ContentItem from a repository.
   Subscribers must rely only on event fields.

4. Add missing AuditAction enum values:
    - UNPUBLISHED
    - RESTORED

5. Use existing AuditAction values where already available:
    - CREATED
    - UPDATED
    - ARCHIVED

6. Wire all five subscribers into:
    - ChroniconRuntime.buildSubscribers()

7. Add focused unit tests for each subscriber, following the current test style for ContentItemPublishedChroniconSubscriber if one exists.

8. Add or update runtime wiring tests if Chronicon has an existing test pattern for subscriber registration.

9. Keep ContentItemDeletedEvent out of this task.
   Do not implement ContentItemDeletedChroniconSubscriber yet.

10. Do not enrich lifecycle events.
    Do not add snapshot support.
    Do not introduce repository lookups.
    Do not modify CodexContentItemService or EventPublishingContentItemService.

Expected result:
- Chronicon writes audit records for create, update, unpublish, archive, and restore lifecycle events.
- All five new subscribers are wired into ChroniconRuntime.
- Existing ContentItemPublishedChroniconSubscriber remains unchanged unless a small consistency refactor is necessary.
- All tests pass.