Create an ADR documenting ContentItem published-only indexing semantics.

Context:
ContentItem lifecycle events are now complete and documented in ADR-008.

codex-index now handles ContentItem lifecycle events as follows:

- ContentItemPublishedEvent    → upsert public index document
- ContentItemUnpublishedEvent  → delete public index document
- ContentItemArchivedEvent     → delete public index document
- ContentItemDeletedEvent      → delete public index document

The index is intentionally published-only. Draft content should not appear in the public index.

Created, updated, and restored events intentionally do not trigger indexing side effects for now.

Goal:
Document the indexing rules so future work does not accidentally index drafts, restored drafts, or working revisions.

Please include:

1. Status
    - Accepted

2. Context
    - ContentItem lifecycle is event-driven.
    - codex-index consumes lifecycle events.
    - The index currently represents public searchable content only.

3. Decision
    - Only published content items are indexed.
    - Publishing a content item upserts an IndexDocument.
    - Unpublishing a content item removes the IndexDocument.
    - Archiving a content item removes the IndexDocument.
    - Deleting a content item removes the IndexDocument.
    - Created, updated, and restored events do not index content.

4. Event behavior table:

   ContentItemCreatedEvent      → no-op
   ContentItemUpdatedEvent      → no-op
   ContentItemPublishedEvent    → upsert
   ContentItemUnpublishedEvent  → delete
   ContentItemArchivedEvent     → delete
   ContentItemRestoredEvent     → no-op
   ContentItemDeletedEvent      → delete

5. Explain why created is no-op:
    - new content starts as DRAFT.

6. Explain why updated is no-op:
    - current update semantics modify the working revision.
    - the public index should not expose draft/working changes.

7. Explain why restored is no-op:
    - restore transitions ARCHIVED → DRAFT.
    - restore does not republish.

8. Explain why unpublished, archived, and deleted remove documents:
    - each transition means the content is no longer public/searchable.

9. Document IndexDocumentId generation:
    - IndexDocumentIds.contentItem(siteKey, contentTypeKey, contentItemKey)
    - format: content-item:{siteKey}:{contentTypeKey}:{contentItemKey}

10. Document projection reload behavior:
- ContentItemPublishedIndexingSubscriber uses ContentItemProjectionSource because it needs full content/revision values.
- Removal subscribers use only event fields and do not reload ContentItem or ContentRevision.
- ContentItemDeletedIndexingSubscriber must not reload because delete is hard delete.

11. Known limitations / future follow-ups:
- no draft index yet
- no preview/editorial index yet
- no reindex-on-update for already published content until future edit/revision semantics are defined
- no bulk rebuild task yet
- no search permission/visibility model yet
- no multi-language/variant index strategy yet

Constraints:
- Documentation only.
- Do not change production code.
- Do not change tests.
- Follow the existing ADR style and numbering convention.