Create a short architecture note or ADR documenting the current ContentItem lifecycle semantics.

Context:
ContentItemService now supports:
- create
- update
- publish
- unpublish
- archive
- restore
- delete

Events and cache invalidation subscribers are wired for the lifecycle.

Goal:
Document the current lifecycle rules and known follow-ups so future tasks do not accidentally change the semantics.

Please include:
1. Valid lifecycle transitions:
    - DRAFT / WORKING creation semantics
    - update behavior
    - publish behavior
    - unpublish behavior
    - archive behavior
    - restore behavior
    - delete behavior
2. Delete semantics:
    - delete is currently a hard delete
    - delete is allowed only from ARCHIVED
    - after delete, findByKey returns Optional.empty()
3. Event publishing:
    - CodexContentItemService does not publish events directly
    - EventPublishingContentItemService publishes lifecycle events after successful delegate operations
4. Cache invalidation:
    - cache invalidation is event-driven through local subscribers
5. Known limitations / future follow-ups:
    - hard delete means audit snapshots must be captured before deletion
    - ContentItemDeletedEvent currently may not be sufficient for rich deletion audit unless it carries a pre-delete snapshot
    - revision orphans are accepted for now
    - future Chronicon/Archivum deletion archival strategy may persist a deleted-content snapshot

Constraints:
- Documentation only.
- Do not change production code unless a typo or broken Javadoc is found.
- Do not change tests.