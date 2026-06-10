Create a Chronicon-local AuditRecordId generator and use it from all ContentItem Chronicon subscribers.

Context:
Chronicon now has audit coverage for all seven ContentItem lifecycle events:

- ContentItemCreatedEvent
- ContentItemUpdatedEvent
- ContentItemPublishedEvent
- ContentItemUnpublishedEvent
- ContentItemArchivedEvent
- ContentItemRestoredEvent
- ContentItemDeletedEvent

All ContentItem Chronicon subscribers currently generate AuditRecordId values using the same convention:

audit:content-item-{action}:{siteKey}:{contentTypeKey}:{contentItemKey}:{occurredAtMillis}

This is consistent, but the ID construction logic is duplicated across subscribers.

Goal:
Centralize AuditRecordId generation in a dedicated Chronicon-local helper/generator, without changing the generated IDs.

Requirements:
1. Inspect all existing ContentItem Chronicon subscribers and confirm the current AuditRecordId format.

2. Create an internal Chronicon helper/generator, for example:
    - AuditRecordIdGenerator
      or
    - AuditRecordIds

3. Add a method specifically for ContentItem lifecycle audit IDs.

   Example shape:

   AuditRecordId contentItemLifecycle(
   String action,
   SiteKey siteKey,
   ContentTypeKey contentTypeKey,
   ContentItemKey contentItemKey,
   Instant occurredAt
   )

   The generated value must remain exactly:

   audit:content-item-{action}:{siteKey}:{contentTypeKey}:{contentItemKey}:{occurredAtMillis}

4. Update all ContentItem Chronicon subscribers to use this new generator/helper.

5. Do not change the resulting AuditRecordId values.

6. Do not change:
    - AuditRecord metadata
    - AuditRecord subject
    - AuditRecord actorId
    - AuditRecord occurredAt
    - AuditRecord summary
    - AuditAction values
    - event payloads
    - event publishing
    - CodexEventDispatcher behavior

7. Do not introduce CodexEventId in this task.

8. Keep the ContentItemDeletedChroniconSubscriber JavaDoc warning that it must not reload the item because delete is a hard delete.

9. Add focused tests for the new AuditRecordId generator/helper:
    - generates expected id for created
    - generates expected id for updated
    - generates expected id for deleted
    - rejects null action
    - rejects null siteKey
    - rejects null contentTypeKey
    - rejects null contentItemKey
    - rejects null occurredAt

10. Existing subscriber tests should keep passing without changing their expected generated ID values, except for minor refactoring if needed.

11. Keep the helper internal to Chronicon for now.

Expected result:
- AuditRecordId generation is centralized.
- All ContentItem Chronicon subscribers use the new helper/generator.
- Generated audit IDs remain unchanged.
- No audit semantics change.
- Full reactor build passes.