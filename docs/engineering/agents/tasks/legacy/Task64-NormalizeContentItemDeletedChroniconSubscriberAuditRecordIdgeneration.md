Normalize ContentItemDeletedChroniconSubscriber AuditRecordId generation.

Context:
We now have symmetric deterministic identity generation for Site, ContentType, and ContentItem.

Chronicon already has audit coverage for all ContentItem lifecycle events.

However, ContentItemDeletedChroniconSubscriber currently builds AuditRecordId using a custom string based on:

siteKey + contentTypeKey + contentItemKey + occurredAtMillis

The existing Chronicon convention is closer to:

audit:{eventType}:{eventId}

Goal:
Make ContentItemDeletedChroniconSubscriber use the same AuditRecordId convention as the other Chronicon subscribers.

Requirements:
1. Inspect the existing ContentItem Chronicon subscribers, especially:
    - ContentItemPublishedChroniconSubscriber
    - ContentItemCreatedChroniconSubscriber
    - ContentItemUpdatedChroniconSubscriber
    - ContentItemArchivedChroniconSubscriber
    - ContentItemRestoredChroniconSubscriber
    - ContentItemUnpublishedChroniconSubscriber

2. Identify the actual AuditRecordId convention currently used by those subscribers.

3. Update ContentItemDeletedChroniconSubscriber to follow that exact same convention.

4. Update ContentItemDeletedChroniconSubscriberTest expectations accordingly.

5. Keep the delete-specific JavaDoc warning:
    - the subscriber must not reload ContentItem
    - delete is a hard delete
    - the item has already been removed when the event is handled

6. Do not enrich ContentItemDeletedEvent.

7. Do not add snapshot support.

8. Do not add a pre-delete archival hook.

9. Do not modify CodexContentItemService or EventPublishingContentItemService.

10. Do not change the AuditRecord action, subject, actor, occurredAt, summary, or metadata unless strictly required for consistency with the other subscribers.

Expected result:
- ContentItemDeletedChroniconSubscriber uses the same AuditRecordId generation style as the rest of the ContentItem Chronicon subscribers.
- The subscriber still relies only on ContentItemDeletedEvent fields.
- Tests pass.