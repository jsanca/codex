Add index removal subscribers for ContentItem lifecycle events that make content non-public.

Context:
codex-index currently has only:

- ContentItemPublishedIndexingSubscriber

It handles ContentItemPublishedEvent by loading the published projection and upserting an IndexDocument.

The index model is published-only. Draft content should not be indexed.

IndexWriter already exposes:
- upsert(IndexDocument)
- delete(IndexDocumentId)

The following events indicate that a previously indexed content item should be removed from the public index:
- ContentItemUnpublishedEvent
- ContentItemArchivedEvent
- ContentItemDeletedEvent

These events carry enough identity data to compute the deterministic IndexDocumentId:

content-item:{siteKey}:{contentTypeKey}:{contentItemKey}

Goal:
Add indexing subscribers that remove documents from the index when content is unpublished, archived, or deleted.

Requirements:
1. Create:
    - ContentItemUnpublishedIndexingSubscriber
    - ContentItemArchivedIndexingSubscriber
    - ContentItemDeletedIndexingSubscriber

2. Each subscriber must:
    - accept its matching event type
    - compute the corresponding IndexDocumentId using event.siteKey(), event.contentTypeKey(), and event.key()
    - call indexWriter.delete(indexDocumentId)

3. Do not use ContentItemProjectionSource.
4. Do not reload ContentItem or ContentRevision from repositories.
5. Do not map an IndexDocument.
6. Do not call indexWriter.upsert(...).
7. Add focused unit tests for each subscriber:
    - constructor null guard
    - handle(null) rejection
    - correct event type if the subscriber API exposes one
    - correct IndexDocumentId is deleted
    - delete is called exactly once

8. Wire all three subscribers into IndexRuntime.buildSubscribers().

9. Update IndexRuntime tests:
    - subscriber count
    - dispatch/integration behavior if such a test exists

10. Consider adding an internal IndexDocumentIds helper if the content-item ID format is duplicated in multiple places.
    If added, it must preserve the existing format exactly:
    content-item:{siteKey}:{contentTypeKey}:{contentItemKey}

11. Do not implement ContentItemRestoredIndexingSubscriber in this task.
12. Do not add subscribers for ContentItemCreatedEvent or ContentItemUpdatedEvent.
13. Do not change ContentItemPublishedIndexingSubscriber behavior.
14. Do not change IndexDocument model.
15. Do not change lifecycle event payloads.

Expected result:
- Unpublished, archived, and deleted content items are removed from the public index.
- No repository/projection reload occurs for removal events.
- IndexRuntime wires publish upsert plus the three removal subscribers.
- All tests pass.