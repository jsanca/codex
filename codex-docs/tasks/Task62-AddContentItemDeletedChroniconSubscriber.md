Task 62 — Add ContentItemDeleted Chronicon subscriber.

Context:
Task 61 added Chronicon audit subscribers for the non-delete ContentItem lifecycle events.

ContentItem deletion is special because ADR-008 defines delete as:
- hard delete
- valid only from ARCHIVED
- after delete, findByKey returns Optional.empty()

EventPublishingContentItemService captures the ContentItem identity before delegating to delete and then dispatches ContentItemDeletedEvent after successful deletion.

Goal:
Add Chronicon audit support for ContentItemDeletedEvent.

Requirements:
1. Create ContentItemDeletedChroniconSubscriber.
2. Follow the mapping style of the existing ContentItem lifecycle Chronicon subscribers.
3. Map ContentItemDeletedEvent to an AuditRecord using AuditAction.DELETED.
4. Do not reload ContentItem from any repository.
5. Add JavaDoc or an inline comment explaining that the subscriber must not reload the item because delete is a hard delete and the item has already been removed.
6. Use only fields available on ContentItemDeletedEvent:
    - id
    - siteKey
    - contentTypeKey
    - contentTypeVersionId
    - key
    - actor
    - occurredAt
7. Wire the subscriber into ChroniconRuntime.buildSubscribers().
8. Add focused unit tests for the subscriber.
9. Update ChroniconRuntime tests to reflect the new subscriber count and event dispatch coverage.
10. Update ConciliumRuntime tests if the total subscriber count changes there.
11. Do not enrich ContentItemDeletedEvent.
12. Do not add snapshot support.
13. Do not add a pre-delete archival hook.
14. Do not modify CodexContentItemService or EventPublishingContentItemService unless a compile issue requires a trivial adjustment.

Expected result:
- ContentItemDeletedEvent creates a DELETED AuditRecord.
- The deleted subscriber is wired into ChroniconRuntime.
- The subscriber does not reload the deleted item.
- Chronicon has audit coverage for all seven ContentItem lifecycle events.
- Full reactor build passes.