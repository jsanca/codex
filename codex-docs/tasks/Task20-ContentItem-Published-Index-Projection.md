# Task20: ContentItem Published Index Projection

## Objective

Implement the first event-driven indexing projection for published content items.

This task connects the existing event pipeline to the new indexing foundation.

When a `ContentItemPublishedEvent` is dispatched, Codex should project the published content item/revision into a neutral `IndexDocument` and write it through an `IndexWriter`.

This task must not implement OpenSearch, Elasticsearch, Lucene, myIR, embeddings, cache invalidation, audit, workflow, or search query APIs.

The goal is to prove the architecture:

`publish content item`
`  -> ContentItemPublishedEvent`
`     -> ContentItemPublishedIndexingSubscriber`
`        -> IndexWriter.upsert(IndexDocument)`

## Architectural direction

Indexing is a projection concern.

Canonical services must not index directly.

Do not modify `CodexContentItemService` to call indexing code.

Do not modify `EventPublishingContentItemService` to call indexing code directly beyond dispatching domain events.

Indexing should be implemented as an event subscriber/projection handler.

The index writer receives a neutral `IndexDocument`.

Backend-specific adapters will come later.

## Scope

Implement:

- event subscriber/listener for `ContentItemPublishedEvent`
- mapping from published content item/revision into `IndexDocument`
- tests using `RecordingIndexWriter`
- integration test proving publish event can produce an index upsert
- documentation note if appropriate

Do not implement:

- OpenSearch writer
- Elasticsearch writer
- Lucene writer
- myIR writer
- embedding writer
- vector search
- cache invalidation
- audit subscriber
- workflow subscriber
- search service
- search query API
- ranking
- filtering
- REST
- Spring integration
- persistence
- TreeableResource
- CodexResourcePath

## Location

Use existing packages where possible.

Suggested packages:

- `codex.codex.internal.index.ContentItemPublishedIndexingSubscriber`
- `codex.codex.internal.index.ContentItemIndexDocumentMapper`
- existing index tests
- existing event/integration tests

If there is already a subscriber/event-listener package, use it.

Do not export internal index packages.

## 1. Event subscription mechanism

Use the existing Codex event dispatch/subscription mechanism if one exists.

If there is already a `CodexEventSubscriber`, annotation-based subscriber, or local dispatcher registration model, use that.

If there is not yet a full subscriber mechanism, implement the smallest internal adapter needed for tests without building a full event bus framework.

Important:

Do not redesign the entire event system in this task.

Do not introduce Kafka, pub/sub, outbox, Spring events, or external brokers.

This task should only prove that a `ContentItemPublishedEvent` can be consumed and projected to an `IndexWriter`.

## 2. Create ContentItemIndexDocumentMapper

Create:

`codex.codex.internal.index.ContentItemIndexDocumentMapper`

Responsibility:

Convert a published content item and its published revision into an `IndexDocument`.

Suggested method:

`IndexDocument toDocument(ContentItem item, ContentRevision revision)`

Requirements:

- validate item non-null
- validate revision non-null
- validate item id matches revision content item id
- validate item siteKey matches revision siteKey
- validate item contentTypeKey matches revision contentTypeKey
- validate item contentTypeVersionId matches revision contentTypeVersionId
- validate revision status is `ContentRevisionStatus.PUBLISHED`
- create deterministic `IndexDocumentId`
- resourceType should be `IndexResourceType.CONTENT_ITEM`
- siteKey should come from the item
- title should be derived from values if possible
- body should be derived from revision values
- fields should include revision values in backend-neutral form
- metadata should include useful identity references
- updatedAt should come from `item.updatedAt()` if available, otherwise `revision.createdAt()`

Do not include backend-specific fields.

Do not include Lucene/OpenSearch mapping concepts.

## 3. IndexDocument id rule

For content items, use a deterministic index document id.

Suggested value:

`content-item:{siteKey}:{contentTypeKey}:{contentItemKey}`

Use existing value objects when building the id.

Example:

`content-item:authoring-site:blog-post:welcome-to-codex`

Do not use random UUIDs.

Do not use revision id as the main index document id for public content item indexing.

Reason:

The public/searchable document represents the current published content item.

When a new revision is published later, it should upsert the same content item index document id.

Revision-specific indexing can be added later with `IndexResourceType.CONTENT_REVISION`.

## 4. Title and body mapping

Keep mapping simple and explicit.

For this task:

- if revision values contain `FieldKey.of("title")`, use that value as `IndexDocument.title`
- otherwise use `ContentItemKey.value()` as title
- build `body` by concatenating string-like values from the revision values map

Rules:

- ignore nulls, although nulls should not exist
- include only simple scalar values for body:
    - String
    - Number
    - Boolean
- convert scalar values to strings
- do not serialize complex objects
- do not implement localization
- do not implement rich text parsing
- do not implement HTML stripping
- do not implement analyzer/tokenization behavior

This is a neutral first projection.

Better mapping policies can come later.

## 5. Fields mapping

`IndexDocument.fields` should include a backend-neutral representation of revision values.

For this task:

- convert `Map<FieldKey, Object>` into `Map<String, Object>`
- use `FieldKey.value()` as the map key
- preserve simple values as-is
- for non-simple values, either:
    - call `toString()`, or
    - skip them

Prefer skipping non-simple values unless current tests require otherwise.

Do not mutate revision values.

Do not expose the original map.

## 6. Metadata mapping

Add useful string metadata.

Suggested metadata keys:

- `contentItemId`
- `contentItemKey`
- `contentTypeKey`
- `contentTypeVersionId`
- `contentRevisionId`
- `revisionNumber`
- `revisionStatus`

Example values:

- `contentItemId` -> `item.id().value()`
- `contentItemKey` -> `item.key().value()`
- `contentTypeKey` -> `item.contentTypeKey().value()`
- `contentTypeVersionId` -> `item.contentTypeVersionId().value()`
- `contentRevisionId` -> `revision.id().value()`
- `revisionNumber` -> `String.valueOf(revision.revisionNumber())`
- `revisionStatus` -> `revision.status().name()`

Metadata values should be strings.

Do not include content field values in metadata.

## 7. Create ContentItemPublishedIndexingSubscriber

Create:

`codex.codex.internal.index.ContentItemPublishedIndexingSubscriber`

Responsibility:

Handle `ContentItemPublishedEvent`, load the required canonical data, map it to `IndexDocument`, and call `IndexWriter.upsert(...)`.

Constructor dependencies:

- `ContentItemRepository`
- `ContentRevisionRepository`
- `IndexWriter`
- `ContentItemIndexDocumentMapper`

Requirements:

- validate constructor dependencies
- validate event non-null
- load content item by event identity
- load published revision by `event.publishedRevisionId()`
- if content item is missing, do not index and fail clearly or ignore based on existing event subscriber conventions
- if revision is missing, do not index and fail clearly or ignore based on existing event subscriber conventions
- map item + revision to `IndexDocument`
- call `indexWriter.upsert(document)`

Recommended behavior for missing canonical data:

Throw `IllegalStateException`.

Reason:

The event says the content item was published. If the canonical item or revision cannot be loaded, the projection is inconsistent and should fail loudly in the in-memory/local implementation.

Later, production subscribers may use retry/dead-letter behavior.

## 8. Subscriber method shape

Follow existing subscriber conventions if present.

Possible shapes:

If there is an interface:

`public final class ContentItemPublishedIndexingSubscriber implements CodexEventSubscriber<ContentItemPublishedEvent>`

or:

`public void on(ContentItemPublishedEvent event)`

If annotation-based subscription exists, use existing annotation style.

If no subscriber convention exists yet, keep it explicit:

`public void handle(ContentItemPublishedEvent event)`

Do not invent a large subscription framework.

The test can call `handle(event)` directly if no dispatcher registration mechanism exists.

## 9. Dispatcher integration

If there is already a local event dispatcher that supports subscriber registration, add a test proving this subscriber can be registered and invoked when `ContentItemPublishedEvent` is dispatched.

If no registration mechanism exists yet, do not build one here.

In that case:

- test mapper directly
- test subscriber directly by calling `handle(event)`
- leave full dispatcher registration for a later event bus task

Do not broaden the scope.

## 10. Runtime wiring

Do not wire indexing into `CodexRuntime.inMemory()` by default unless the runtime already supports optional subscribers/index writers cleanly.

For this task, prefer direct tests with `RecordingIndexWriter`.

If runtime wiring is simple and already has an event bus/subscriber registry, optional wiring may be added.

Do not force `CodexRuntime` to expose indexing internals.

Do not add OpenSearch/myIR/embedding configuration.

## 11. Tests

Add plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repositories and `RecordingIndexWriter`.

### Mapper tests

Create tests for `ContentItemIndexDocumentMapper`.

Test cases:

1. maps content item and published revision to index document
    - resourceType is `CONTENT_ITEM`
    - siteKey is preserved
    - id is deterministic
    - title is taken from `title` field
    - body contains string values
    - fields include simple values
    - metadata includes item/revision identities

2. uses content item key as title when title field is missing

3. rejects null item

4. rejects null revision

5. rejects item/revision content item mismatch

6. rejects item/revision siteKey mismatch

7. rejects item/revision contentTypeKey mismatch

8. rejects non-published revision

9. does not dump complex values into metadata

### Subscriber tests

Create tests for `ContentItemPublishedIndexingSubscriber`.

Use:

- `MemoryContentItemRepository`
- `MemoryContentRevisionRepository`
- `RecordingIndexWriter`
- `ContentItemIndexDocumentMapper`

Test cases:

1. handle published event loads item and revision and upserts document

2. upserted document has expected id and resource type

3. handle rejects null event

4. handle throws when content item is missing

5. handle throws when revision is missing

6. handle does not write to index when content item is missing

7. handle does not write to index when revision is missing

### Integration-style test

If simple, add one integration-style test:

Flow:

1. create content type
2. add required title field
3. activate content type
4. create content item
5. publish content item
6. get resulting `ContentItemPublishedEvent` from recorded events
7. pass event to `ContentItemPublishedIndexingSubscriber`
8. assert `RecordingIndexWriter` contains one upserted `IndexDocument`

This test should not require OpenSearch, myIR, or external systems.

If extracting the event from runtime is awkward, create the event manually from the published item for this task.

Do not modify runtime just to support this integration test.

## 12. Error handling

For this MVP:

- projection errors may throw runtime exceptions
- no retries
- no dead letter queue
- no partial failure handling
- no asynchronous executor changes

Production-grade retry/error handling will come later.

## 13. Documentation

Add or update a short note explaining:

- `ContentItemPublishedEvent` is the natural trigger for public content indexing
- indexing is event-driven
- canonical services do not index directly
- `ContentItemPublishedIndexingSubscriber` is the first projection subscriber
- `IndexDocument` is backend-neutral
- actual backends such as OpenSearch, myIR, Lucene, and embeddings are future adapters
- search/query APIs are future work

If ADR-007 exists, optionally update its Future Work or Notes section:

- indexing foundation exists
- first content item published projection exists

Do not create a large new ADR unless project convention requires it.

## Constraints

- Follow CLAUDE.md conventions.
- Keep indexing backend-neutral.
- Use `IndexWriter`.
- Use `IndexDocument`.
- Do not modify canonical services for indexing.
- Do not add OpenSearch.
- Do not add Elasticsearch.
- Do not add Lucene.
- Do not add myIR adapter.
- Do not add embeddings.
- Do not add vector database support.
- Do not add cache invalidation.
- Do not add audit.
- Do not add workflow.
- Do not add search query service.
- Do not add REST.
- Do not add Spring.
- Do not add JPA.
- Do not add persistence framework.
- Do not add TreeableResource.
- Do not add CodexResourcePath.
- Do not broadly refactor event dispatching.
- Do not broadly refactor runtime wiring.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable classes.
- Run the smallest relevant Maven test command after implementation.