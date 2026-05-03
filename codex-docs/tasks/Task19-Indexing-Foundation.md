# Task19: Indexing Foundation

## Objective

Introduce the first framework-agnostic indexing foundation for Codex.

This task should define the neutral indexing contracts and model objects that future subscribers can use to project Codex resources into different search/index backends.

This task should not implement OpenSearch, Elasticsearch, myIR, embeddings, cache invalidation, audit, workflow, or search query APIs.

The goal is to create a small indexing foundation that can later support:

- OpenSearch
- Elasticsearch
- Lucene
- Codex/myIR
- embeddings/vector search
- in-memory test indexes
- no-op indexes
- future hybrid search

## Architectural direction

Indexing is a projection concern.

Canonical services should not index directly.

Canonical services should emit domain events.

Indexing subscribers will later consume those events and write neutral index documents through an `IndexWriter`.

Do not inject index writers into canonical services.

Do not modify `CodexContentItemService`, `CodexContentTypeService`, or `CodexSiteService` for indexing in this task.

The boundary is:

`domain operation`
`  -> domain event`
`     -> future indexing subscriber`
`        -> IndexWriter`

This task only creates the foundation needed for the future subscriber.

## Scope

Implement:

- `IndexDocumentId`
- `IndexResourceType`
- `IndexDocument`
- `IndexWriter`
- `NoOpIndexWriter`
- `RecordingIndexWriter`
- tests
- documentation note or ADR update if appropriate

Do not implement:

- ContentItem indexing subscriber
- ContentItemPublishedEvent subscriber
- OpenSearch integration
- Elasticsearch integration
- Lucene integration
- myIR adapter
- embedding/vector indexing
- cache invalidation
- audit
- workflow continuation
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

Use a new indexing package.

Suggested packages:

- `codex.codex.api.index`
- `codex.codex.internal.index`

API-level contracts and value objects should live under:

`codex.codex.api.index`

Test/helper implementations may live under:

`codex.codex.internal.index`

Suggested files:

- `codex.codex.api.index.IndexDocumentId`
- `codex.codex.api.index.IndexResourceType`
- `codex.codex.api.index.IndexDocument`
- `codex.codex.api.index.IndexWriter`
- `codex.codex.internal.index.NoOpIndexWriter`
- `codex.codex.internal.index.RecordingIndexWriter`

If module exports are required, update `module-info.java` only for API packages that must be visible to external modules.

Do not export internal packages.

## 1. Create IndexDocumentId

Create:

`codex.codex.api.index.IndexDocumentId`

Requirements:

- use a Java record
- wrap a `String value`
- validate non-null
- trim value
- validate non-blank
- provide static factory method:

`of(String value)`

Suggested deterministic formats for future use:

- `site:{siteKey}`
- `content-type:{siteKey}:{contentTypeKey}`
- `content-item:{siteKey}:{contentTypeKey}:{contentItemKey}`
- `content-revision:{siteKey}:{contentTypeKey}:{contentItemKey}:r{revisionNumber}`

Do not implement all those factories unless simple and useful.

At minimum provide `of(String value)`.

## 2. Create IndexResourceType

Create:

`codex.codex.api.index.IndexResourceType`

Use an enum.

Initial values:

- `SITE`
- `CONTENT_TYPE`
- `CONTENT_ITEM`
- `CONTENT_REVISION`
- `CONTENT_TYPE_VERSION`

Semantics:

- `SITE`: a site resource
- `CONTENT_TYPE`: a content type definition
- `CONTENT_TYPE_VERSION`: a published content type schema snapshot
- `CONTENT_ITEM`: a logical content item, usually projected from its published revision
- `CONTENT_REVISION`: a specific content revision snapshot

Do not overbuild type metadata yet.

Do not add external resource protocols in this task.

## 3. Create IndexDocument

Create:

`codex.codex.api.index.IndexDocument`

Use a Java record.

Suggested shape:

`IndexDocumentId id`
`IndexResourceType resourceType`
`SiteKey siteKey`
`String title`
`String body`
`Map<String, Object> fields`
`Map<String, String> metadata`
`Instant updatedAt`

Requirements:

- `id` must not be null
- `resourceType` must not be null
- `siteKey` must not be null
- `title` may be null but should default to empty string
- `body` may be null but should default to empty string
- `fields` may be null and should default to `Map.of()`
- `metadata` may be null and should default to `Map.of()`
- `updatedAt` may be null and should default to `Instant.now()`
- trim `title`
- trim `body`
- defensively copy `fields` with `Map.copyOf(...)`
- defensively copy `metadata` with `Map.copyOf(...)`
- reject null field keys
- reject null field values
- reject null metadata keys
- reject null metadata values
- add builder support if consistent with project style
- add `copyOf(...)` if consistent with project style
- `toString()` should not dump large fields/body
- `toString()` should include:
    - id
    - resourceType
    - siteKey
    - title
    - field count
    - metadata count
    - updatedAt

Do not include embeddings in this task.

Do not include backend-specific properties in this task.

Do not include OpenSearch mapping concerns in this task.

## 4. IndexDocument design notes

`IndexDocument` is a neutral projection document.

It is not a domain entity.

It should be suitable for translation into:

- OpenSearch documents
- Elasticsearch documents
- Lucene documents
- myIR documents
- embedding input text
- in-memory test projections

Fields:

`title`
- short human-readable label
- useful for admin search and result display

`body`
- full searchable text
- may be built from published content values in a future subscriber

`fields`
- structured values useful for filtering/sorting/backend-specific indexing
- should remain backend-neutral

`metadata`
- string metadata for tracing/debugging
- examples:
    - `contentTypeKey`
    - `contentItemKey`
    - `contentTypeVersionId`
    - `contentRevisionId`

Do not use `IndexDocument` as canonical storage.

## 5. Create IndexWriter

Create:

`codex.codex.api.index.IndexWriter`

Interface contract:

`void upsert(IndexDocument document)`

`void delete(IndexDocumentId id)`

Requirements:

- validate arguments in implementations
- interface should remain small
- no checked exceptions for now
- no backend-specific APIs
- no search methods here

This is a write-side projection contract only.

Do not add `search(...)` to `IndexWriter`.

Search will be modeled separately later.

## 6. Create NoOpIndexWriter

Create:

`codex.codex.internal.index.NoOpIndexWriter`

Requirements:

- final class
- implements `IndexWriter`
- `upsert(...)` validates document non-null and does nothing
- `delete(...)` validates id non-null and does nothing
- useful for runtime wiring where indexing is disabled

No logging required.

No external infrastructure.

## 7. Create RecordingIndexWriter

Create:

`codex.codex.internal.index.RecordingIndexWriter`

Requirements:

- final class
- implements `IndexWriter`
- records upserted documents
- records deleted ids
- useful for tests
- should be thread-safe enough for current event usage
- use simple synchronization or concurrent collections
- expose immutable snapshots:

`List<IndexDocument> upserts()`

`List<IndexDocumentId> deletes()`

- expose clear method:

`void clear()`

Requirements:

- `upsert(...)` rejects null document
- `delete(...)` rejects null id
- snapshots should be immutable
- do not expose mutable internal collections

Ordering:

- preserve insertion order if simple to do safely
- if using synchronized lists, document the behavior

## 8. Do not add subscribers yet

Do not implement:

- `ContentItemIndexingSubscriber`
- `ContentItemPublishedIndexingSubscriber`
- `SiteIndexingSubscriber`
- `ContentTypeIndexingSubscriber`

Those are future tasks.

This task only provides the indexing model and writer abstraction.

## 9. Do not add search service yet

Do not create:

- `SearchService`
- `ContentSearchService`
- `CodexSearchService`
- `SearchQuery`
- `SearchResult`
- `SearchHit`

Those will be created later after indexing projections exist.

## 10. Do not change canonical services

Do not modify canonical services for indexing:

- do not modify `CodexSiteService`
- do not modify `CodexContentTypeService`
- do not modify `CodexContentItemService`

Do not inject `IndexWriter` into those services.

Do not call indexing code from publish/create/activate methods.

Indexing will be event-driven later.

## 11. Tests

Add plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

### IndexDocumentId tests

Add tests for:

- rejects null value
- rejects blank value
- trims value
- `of(...)` factory works
- equality works by value

### IndexResourceType tests

Add tests or compile-level coverage for:

- `SITE`
- `CONTENT_TYPE`
- `CONTENT_TYPE_VERSION`
- `CONTENT_ITEM`
- `CONTENT_REVISION`

### IndexDocument tests

Add tests for:

- rejects null id
- rejects null resourceType
- rejects null siteKey
- defaults null title to empty string
- defaults null body to empty string
- trims title
- trims body
- defaults null fields to empty map
- defaults null metadata to empty map
- defensively copies fields
- defensively copies metadata
- fields accessor is immutable
- metadata accessor is immutable
- rejects null field keys
- rejects null field values
- rejects null metadata keys
- rejects null metadata values
- defaults updatedAt when null
- builder supports all fields if builder exists
- copyOf preserves fields if copyOf exists
- toString does not dump full body

### NoOpIndexWriter tests

Add tests for:

- `upsert` accepts valid document
- `delete` accepts valid id
- `upsert` rejects null document
- `delete` rejects null id

### RecordingIndexWriter tests

Add tests for:

- records upserted document
- records deleted id
- upserts snapshot is immutable
- deletes snapshot is immutable
- clear removes recorded operations
- upsert rejects null document
- delete rejects null id
- preserves insertion order if implemented

## 12. Documentation

Add or update a short note explaining:

- indexing is a projection concern
- canonical services do not index directly
- `IndexWriter` is write-side only
- `IndexDocument` is a neutral projection document
- search/query APIs are future work
- OpenSearch, myIR, embeddings, and other backends are future adapters
- event subscribers will later translate domain events into index documents

If ADR-007 exists, optionally update its Future Work section to mention that the indexing foundation now exists.

Do not create a large ADR unless the project convention requires one for this kind of foundation.

## 13. Module info

If `codex.codex.api.index` is introduced and external modules need it, update `module-info.java` to export:

`codex.codex.api.index`

Do not export internal index packages.

Ensure tests still compile with Java modules.

## Constraints

- Follow CLAUDE.md conventions.
- Keep the indexing foundation backend-neutral.
- No OpenSearch.
- No Elasticsearch.
- No Lucene.
- No myIR adapter.
- No embeddings.
- No vector database.
- No cache.
- No audit.
- No workflow.
- No search query service.
- No subscribers.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No Custos implementation.
- No TreeableResource implementation.
- No CodexResourcePath implementation.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable classes.
- Run the smallest relevant Maven test command after implementation.