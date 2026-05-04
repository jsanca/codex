# Task 23 Post-Task Report — Content Item Projection Source

## Files Changed

### New files
- `codex-codex/src/main/java/codex/codex/internal/index/ContentItemProjectionSource.java`
  — Interface with two methods: `loadItem(event)` and `loadPublishedRevision(event)`.
- `codex-codex/src/main/java/codex/codex/internal/index/RepositoryContentItemProjectionSource.java`
  — MVP implementation backed by `ContentItemRepository` + `ContentRevisionRepository`.
  Throws `IllegalStateException` on missing item or revision (system invariant violation, not domain error).
- `codex-codex/src/test/java/codex/codex/internal/index/RepositoryContentItemProjectionSourceTest.java`
  — 8 focused unit tests for the new abstraction:
  constructor null guards (×2), `loadItem` (null guard, happy path, missing → throws),
  `loadPublishedRevision` (null guard, happy path, missing → throws).

### Modified files
- `codex-codex/src/main/java/codex/codex/internal/index/ContentItemPublishedIndexingSubscriber.java`
  — Old constructor accepted `(ContentItemRepository, ContentRevisionRepository, IndexWriter, mapper)`.
  New constructor accepts `(ContentItemProjectionSource, IndexWriter, ContentItemIndexDocumentMapper)`.
  Subscriber now delegates entirely to the projection source; no direct repository access.
- `codex-codex/src/main/java/codex/codex/internal/runtime/CodexRuntime.java`
  — `inMemory(IndexWriter)` now constructs `RepositoryContentItemProjectionSource` and passes it
  into the subscriber instead of passing the repositories directly.
- `codex-codex/src/test/java/codex/codex/internal/index/ContentItemPublishedIndexingSubscriberTest.java`
  — Rewritten to wire through `RepositoryContentItemProjectionSource` rather than raw repositories.
  14 tests covering constructor null guards, handle happy path, missing item/revision throw,
  dispatcher integration, and `CodexEventSubscriber` contract.
- `codex-codex/src/test/java/codex/codex/internal/index/ContentItemIndexingIntegrationTest.java`
  — Updated constructor call site to match the new subscriber signature (projectionSource as first arg).

## Tests Run

```
mvn test -pl codex-fundamentum,codex-codex -am -q
```

All tests passed. No failures, no errors.

## Intentional Deviations

None. Task was implemented as specified.

## Architectural Questions Discovered

- **Future shape of `ContentItemProjectionSource`**: The interface today carries a hard dependency on
  `ContentItemPublishedEvent`. If the system later needs projection sources for other event types
  (e.g., `ContentItemUnpublishedEvent`), either the interface will grow additional load methods or
  it will need to be parameterized by event type. No action needed now — record for near-future design.
- **`ContentRevisionRepository.findById` vs `findByPublishedRevisionId`**: Currently `loadPublishedRevision`
  finds the revision by the id the event carries (`publishedRevisionId`). This relies on the event
  containing the exact revision id stored in the repository. Works for the in-memory case; may need
  attention when real persistence is wired.

## Follow-up Cleanup Recommended

- **Exception migration**: Existing domain exceptions (e.g., site, content-type) may still live in
  `internal.service` packages from earlier tasks. The exception conventions established in Task 23's
  retro require them in `api.exception`. Recommend a dedicated cleanup task to audit and migrate.
- **`RepositoryContentItemProjectionSource` future-forward javadoc**: The class contains a comment
  sketching the read-only unit-of-work shape. Remove or promote when that abstraction is actually built.
