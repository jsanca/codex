# Task23: Content Item Projection Source

## Objective

Decouple indexing subscribers from direct repository access.

`ContentItemPublishedIndexingSubscriber` currently needs to load the canonical `ContentItem` and published `ContentRevision` before mapping them to an `IndexDocument`.

Direct repository access works for the current in-memory MVP, but it creates future concerns around:

- cache-aware reads
- read-only transaction/unit-of-work boundaries
- connection/session lifecycle
- projection read models
- future batching/indexing queues
- keeping subscribers small and focused

This task introduces a small projection read-source abstraction used by indexing subscribers.

The indexing subscriber should depend on a projection source, mapper, and index writer — not directly on repositories.

## Architectural direction

Subscribers should be specialized and small.

A subscriber should react to one event and perform one projection responsibility.

For content item indexing:

```text
ContentItemPublishedEvent
  -> ContentItemPublishedIndexingSubscriber
     -> ContentItemProjectionSource
     -> ContentItemIndexDocumentMapper
     -> IndexWriter
```
The subscriber should not know whether the source loads data from:

repositories
cache
database session
read-only unit of work
read model
external projection store

For the current MVP, implement a repository-backed projection source.

Future work may wrap this source with read-only transactions, cache, batching, or queueing.

Scope

Implement:

ContentItemProjectionSource
RepositoryContentItemProjectionSource
update ContentItemPublishedIndexingSubscriber to use the source
tests

Do not implement:

read-only unit of work
transaction manager
cache
indexing queue
batching
indexing policy
retry/dead-letter
audit subscriber
observability subscriber
cache invalidation subscriber
workflow subscriber
OpenSearch/myIR/Lucene/embedding adapters
search service
Spring integration
persistence framework
Location

Suggested package:

codex.codex.internal.index.ContentItemProjectionSource
codex.codex.internal.index.RepositoryContentItemProjectionSource
codex.codex.internal.index.ContentItemPublishedIndexingSubscriber

This is internal infrastructure for projections.

Do not export this package.

1. Create ContentItemProjectionSource

Create:

codex.codex.internal.index.ContentItemProjectionSource

Interface methods:

ContentItem loadItem(ContentItemPublishedEvent event)

ContentRevision loadPublishedRevision(ContentItemPublishedEvent event)

Requirements:

input is the domain event
return canonical objects required for projection
implementations should validate event non-null
method names should make intent clear
no cache implementation
no transaction implementation
no indexing behavior
no mapping behavior

JavaDoc should explain:

this is a read source for content item projections
it exists so subscribers do not depend directly on repositories
future implementations may use cache, read-only unit of work, or read models
2. Create RepositoryContentItemProjectionSource

Create:

codex.codex.internal.index.RepositoryContentItemProjectionSource

Constructor dependencies:

ContentItemRepository
ContentRevisionRepository

Behavior:

loadItem(ContentItemPublishedEvent event)

validate event non-null
load item by:
event.siteKey()
event.contentTypeKey()
event.key()
if missing, throw IllegalStateException

loadPublishedRevision(ContentItemPublishedEvent event)

validate event non-null
load revision by:
event.publishedRevisionId()
if missing, throw IllegalStateException

Exception message examples:

Content item not found for published event: {siteKey}/{contentTypeKey}/{key}
Published revision not found: {publishedRevisionId}

Requirements:

no indexing behavior
no mapping behavior
no event dispatching
no cache
no transaction handling
no Spring
no persistence-specific code
3. Update ContentItemPublishedIndexingSubscriber

Update:

ContentItemPublishedIndexingSubscriber

Current dependencies likely include:

ContentItemRepository
ContentRevisionRepository
IndexWriter
ContentItemIndexDocumentMapper

Replace repository dependencies with:

ContentItemProjectionSource
IndexWriter
ContentItemIndexDocumentMapper

New behavior:

handle(event)
-> validate event
-> item = projectionSource.loadItem(event)
-> revision = projectionSource.loadPublishedRevision(event)
-> document = mapper.toDocument(item, revision)
-> indexWriter.upsert(document)

Requirements:

constructor validates projection source, index writer, mapper
no direct repository access in the subscriber
preserve existing logging
preserve existing event type implementation if it implements CodexEventSubscriber<ContentItemPublishedEvent>
preserve current exception behavior via source
do not add read-only unit of work yet
4. Future-forward note for read-only unit of work

Do not implement this now, but add JavaDoc or documentation note showing the intended future shape:

readOnlyUnitOfWork.call(() -> {
ContentItem item = source.loadItem(event);
ContentRevision revision = source.loadPublishedRevision(event);
IndexDocument document = mapper.toDocument(item, revision);
indexWriter.upsert(document);
});

This is only documentation.

Do not create ReadOnlyUnitOfWork in this task.

5. Update runtime wiring

If CodexRuntime wires ContentItemPublishedIndexingSubscriber, update it to create:

RepositoryContentItemProjectionSource
-> ContentItemPublishedIndexingSubscriber

Use the same repositories already used by the canonical services.

Do not create duplicate repositories.

Expected construction shape:

ContentItemProjectionSource projectionSource =
new RepositoryContentItemProjectionSource(contentItemRepository, contentRevisionRepository);

ContentItemPublishedIndexingSubscriber subscriber =
new ContentItemPublishedIndexingSubscriber(projectionSource, indexWriter, mapper);

Do not change event dispatcher behavior.

Do not change CodexEventDispatcher.

6. Tests

Add or update plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repositories.

ContentItemProjectionSource tests

For RepositoryContentItemProjectionSource, add tests:

constructor rejects null content item repository
constructor rejects null content revision repository
loadItem rejects null event
loadPublishedRevision rejects null event
loadItem returns existing item
loadPublishedRevision returns existing revision
loadItem throws IllegalStateException when item is missing
loadPublishedRevision throws IllegalStateException when revision is missing
ContentItemPublishedIndexingSubscriber tests

Update existing tests:

constructor rejects null projection source
constructor rejects null index writer
constructor rejects null mapper
handle rejects null event
handle uses projection source to load item and revision
handle maps item + revision to document
handle writes one upsert to IndexWriter
missing item behavior still throws
missing revision behavior still throws
when source throws, no document is indexed

If existing tests used repositories directly, update them to either:

test RepositoryContentItemProjectionSource separately
or wire the subscriber through the projection source
Runtime/integration tests

Update any runtime indexing tests so wiring still works.

Specifically, tests that prove:

publish -> ContentItemPublishedEvent -> subscriber -> RecordingIndexWriter.upsert(...)

should continue to pass.

7. Documentation

Add or update a short note explaining:

indexing subscribers should not depend directly on repositories
projection sources are read facades for event projections
repository-backed source is the current MVP implementation
future projection sources may use cache, read-only unit of work, or read models
indexing remains event-driven
audit, observability, cache invalidation, and workflow should be separate subscribers

If ADR-007 exists, optionally add a note under event-driven projections.

8. Design notes

This task intentionally does not use ContentItemService.

Reason:

ContentItemService owns lifecycle operations and canonical commands.

Projection subscribers need a read source for projection data, not the lifecycle service.

This prevents ContentItemService from becoming a god service and keeps search/indexing concerns separate from content lifecycle.

9. Constraints
   Follow CLAUDE.md conventions.
   Keep subscribers focused on one responsibility.
   Do not use direct repositories inside ContentItemPublishedIndexingSubscriber.
   Use ContentItemProjectionSource.
   Do not implement cache.
   Do not implement read-only unit of work.
   Do not implement indexing queue.
   Do not implement indexing policy.
   Do not implement audit.
   Do not implement observability.
   Do not implement cache invalidation.
   Do not implement workflow.
   Do not implement OpenSearch.
   Do not implement myIR adapter.
   Do not implement Lucene adapter.
   Do not implement embeddings.
   Do not implement search service.
   Do not introduce Spring.
   Do not introduce persistence framework.
   Do not modify unrelated files.
   Do not modify .idea, target, build, or generated files.
   Keep comments and JavaDoc in English.
   Prefer small, explicit, testable classes.
   Run the smallest relevant Maven test command after implementation.