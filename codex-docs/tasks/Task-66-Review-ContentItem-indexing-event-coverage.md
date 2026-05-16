Review the current codex-index support for ContentItem lifecycle events.

Context:
Codex Core now publishes lifecycle events for ContentItem:
- ContentItemCreatedEvent
- ContentItemUpdatedEvent
- ContentItemPublishedEvent
- ContentItemUnpublishedEvent
- ContentItemArchivedEvent
- ContentItemRestoredEvent
- ContentItemDeletedEvent

Cache invalidation and Chronicon audit already consume these events.

Goal:
Inspect codex-index and report the current indexing support for ContentItem lifecycle events.

Please check:
1. Which ContentItem lifecycle events currently have indexing subscribers.
2. Which subscribers are wired into the index runtime.
3. What the current index document model looks like.
4. Whether indexing subscribers currently reload ContentItem / ContentRevision from repositories or rely only on event fields.
5. Whether the index represents only public/published content or also drafts.
6. What operation exists on the index API:
    - upsert
    - delete/remove
    - mark inactive
    - rebuild
7. Whether ContentItemPublishedEvent carries enough data to index a document.
8. Whether ContentItemUnpublishedEvent, ContentItemArchivedEvent, and ContentItemDeletedEvent carry enough data to remove a document.
9. Recommend the smallest safe implementation task.

Constraints:
- Inspection/report only.
- Do not implement subscribers yet.
- Do not change event payloads.
- Do not change ContentItem lifecycle semantics.
- Do not change Chronicon or cache invalidation.
- Do not introduce a new search/index document model unless reporting that it is missing.

Expected output:
- Current codex-index coverage summary.
- Missing event coverage list.
- Current index API summary.
- Recommendation for the first implementation task.