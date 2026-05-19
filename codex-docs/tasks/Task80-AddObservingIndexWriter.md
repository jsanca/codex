Add an ObservingIndexWriter decorator using the Observance API.

Context:
codex-fundamentum provides Observance, Counter, Timer, NoOpObservance, and InMemoryObservance.

LocalCodexEventDispatcher and DeferredEventDispatcher are already instrumented.

codex-index currently has IndexWriter with:
- upsert(IndexDocument)
- delete(IndexDocumentId)

ContentItem indexing subscribers use IndexWriter to:
- upsert on ContentItemPublishedEvent
- delete on ContentItemUnpublishedEvent
- delete on ContentItemArchivedEvent
- delete on ContentItemDeletedEvent

Goal:
Add an ObservingIndexWriter decorator that measures IndexWriter operations without changing indexing semantics.

Requirements:
1. Create ObservingIndexWriter as a decorator over IndexWriter.

2. Constructor dependencies:
    - IndexWriter delegate
    - Observance observance

3. Validate constructor args with Objects.requireNonNull.

4. Instrument upsert(IndexDocument):
    - increment counter: index.upsert.calls
    - time operation: index.upsert.duration
    - if delegate throws, increment: index.upsert.failures
    - preserve exception behavior

5. Instrument delete(IndexDocumentId):
    - increment counter: index.delete.calls
    - time operation: index.delete.duration
    - if delegate throws, increment: index.delete.failures
    - preserve exception behavior

6. Duration must be recorded for both success and failure.

7. Do not change IndexWriter interface.

8. Do not change existing indexing subscribers.

9. Do not add external dependencies.

10. Add focused tests:
- upsert increments calls
- upsert records duration
- upsert failure increments failures and propagates exception
- delete increments calls
- delete records duration
- delete failure increments failures and propagates exception
- constructor rejects null delegate
- constructor rejects null observance

11. If metric-name strings are repeated, add a package-private helper in codex-index, for example:
- IndexMetricNames

12. Do not wire ObservingIndexWriter into runtime yet unless there is already a clear writer-construction point.
    If runtime wiring is obvious and local, report it as a recommended follow-up instead of doing it in this task.

Expected result:
- IndexWriter can be wrapped with ObservingIndexWriter.
- Index operation calls, durations, and failures can be measured.
- No indexing behavior changes.
- Tests pass.