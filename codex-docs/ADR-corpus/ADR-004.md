# ADR-007: Canonical Services, Cache, Indexing, and Search Boundaries

## Status

Draft

## Context

Codex now has a growing domain model and service pipeline for:

- `Site`
- `ContentType`
- `ContentTypeVersion`
- `ContentItem`
- `ContentRevision`

The current service model separates domain behavior from infrastructure concerns using decorators such as event publishing services.

As Codex evolves, several read/write concerns must be handled carefully:

- canonical domain writes
- identity-based reads
- cache acceleration
- search and discovery
- indexing projections
- audit
- future external systems such as OpenSearch, Lucene/myIR, embeddings, Redis, Kafka, or other mechanisms

A key architectural risk is turning domain services into god services that handle lifecycle, validation, search, indexing, cache, audit, and external integration all at once.

Codex should avoid that.

## Decision

Codex will separate these responsibilities:

```text
Canonical service
  owns domain behavior, validation, lifecycle, and writes

Cache
  accelerates canonical identity reads

Indexing
  updates search/discovery projections from events

Search
  queries indexes/projections, not canonical lifecycle services
```
The canonical service must not depend directly on search indexes, OpenSearch, embeddings, Redis, or cache providers.

For example:

CodexContentItemService
owns content item lifecycle and validation

ContentSearchService
owns search/discovery queries

Indexing subscribers
update search indexes after domain events

Cache subscribers
invalidate/update cache after domain events
Canonical Domain Services

Services such as:

CodexSiteService
CodexContentTypeService
CodexContentItemService

are considered the canonical/domain implementations.

They own:

domain validation
lifecycle transitions
repository writes
required lookups for correctness
semantic exceptions
deterministic model changes

They should use canonical repositories as the source of truth.

They should not use search indexes to decide whether a write is valid.

They should not call OpenSearch, embeddings, Redis, or external projection systems directly.

Write Pipeline

For write operations, Codex will continue using decorator composition.

A future write pipeline may look like:

TransactionalContentItemService
-> LockingContentItemService
-> EventPublishingContentItemService
-> CodexContentItemService
-> Repository

The canonical service performs the domain operation and writes to the canonical repository.

Events are emitted by the event publishing decorator.

DeferredEventDispatcher ensures that events are delivered only after the transaction context commits.

Cache and Writes

Cache should not be the source of truth for writes.

Write operations should not rely on cache values for correctness-sensitive validation unless the cache is explicitly part of a coherent transactional read-through strategy.

For Codex MVP, write validation should load from canonical repositories.

Cache should be updated or invalidated after successful domain events.

Example:

ContentItemPublishedEvent
-> CacheInvalidationSubscriber
-> IndexingSubscriber
-> AuditSubscriber

This keeps cache coherent without coupling domain services to cache infrastructure.

Cache and Reads

Cache may be used to accelerate identity-based reads.

Examples of good cache candidates:

find Site by SiteKey
find ContentType by SiteKey + ContentTypeKey
find ContentItem by SiteKey + ContentTypeKey + ContentItemKey
find ContentRevision by ContentRevisionId
find ContentTypeVersion by ContentTypeVersionId

These are canonical lookups by stable identity.

A future read wrapper may look like:

CachingContentItemService
-> CodexContentItemService
-> Repository

or a repository-level cache adapter may exist:

CachingContentItemRepository
-> DatabaseContentItemRepository

The first implementation should prefer a service wrapper unless repository-level caching becomes clearly more appropriate.

Do not cache broad or unstable queries by default:

findAll
findByContentType
arbitrary filters
full-text search
similarity search
embedding search

Those belong more naturally to search/index projections.

Search Is Separate From Lifecycle Services

ContentItemService should not become responsible for search.

Its responsibility is content item lifecycle and canonical identity lookups.

Search/discovery should live in a separate service, such as:

ContentSearchService

or a more general future service:

CodexSearchService

Search services may use:

OpenSearch
Elasticsearch
Lucene
myIR
vector databases
embeddings
hybrid search
in-memory projections

Search services query projections/indexes. They should not be required for canonical writes to succeed.

Indexing

Indexing is a projection concern.

Codex should not index directly inside canonical services.

Instead:

domain operation
-> event
-> indexing subscriber
-> index writer

Example:

ContentItemPublishedEvent
-> ContentItemIndexingSubscriber
-> OpenSearchIndexWriter
-> EmbeddingIndexWriter
-> LocalIrIndexWriter

This allows multiple indexing mechanisms to subscribe independently.

Potential indexing backends:

OpenSearch
Elasticsearch
Lucene
Codex/myIR
embedding/vector index
in-memory test index
no-op index
Public Search vs Administrative Search

Not all resources are indexed for the same purpose.

Examples:

Site
administrative discovery
runtime lookup
not necessarily public search

ContentType
schema discovery
Olórin/agent awareness
administrative search

ContentItem
public/editorial search when published
admin search when draft

ContentRevision
usually not indexed directly for public search
published revision may be projected as the searchable ContentItem document

Public content search should generally be driven by published content events, not draft creation.

For example:

ContentItemCreatedEvent
useful for audit/admin/event history

ContentItemPublishedEvent
useful for public indexing
Event-Driven Projections

Indexing, cache invalidation, audit, and workflow continuation should be implemented as event subscribers or projection handlers.

They should not be hardcoded into canonical domain services.

Future subscribers may include:

CacheInvalidationSubscriber
AuditSubscriber
ContentItemIndexingSubscriber
ContentTypeProjectionSubscriber
WorkflowContinuationSubscriber
ExternalBrokerPublisher

This keeps the service layer focused and allows infrastructure concerns to evolve independently.

Search Query Layer

A future search abstraction may include:

SearchQuery
SearchResult
SearchHit
SearchService
IndexDocument
IndexWriter

This ADR does not implement those types.

The important decision is the boundary:

Lifecycle services write canonical state.
Event subscribers update projections.
Search services query projections.
Consequences
Positive
Avoids god services.
Keeps domain services focused on lifecycle and validation.
Keeps indexing infrastructure replaceable.
Allows OpenSearch, myIR, embeddings, or other mechanisms to coexist.
Makes cache a performance concern, not a correctness dependency.
Supports after-commit indexing through existing event infrastructure.
Keeps search/discovery separate from canonical writes.
Fits the existing decorator/event pipeline.
Tradeoffs
Search results may be eventually consistent.
Cache invalidation must be designed carefully.
Event subscribers introduce operational complexity.
Some read paths may require deciding between canonical lookup and search lookup.
More components exist than in a single god service model.
Guiding Rules
Writes and domain validation use canonical repositories.

Identity reads may use cache.

Search and discovery use indexes.

Indexes are updated from events.

Cache is invalidated or updated from events.

Canonical services do not depend on OpenSearch, embeddings, Redis, or external search providers.

ContentItemService owns lifecycle, not search.

ContentSearchService owns search/discovery, not lifecycle.
Future Work

Potential future tasks:

Task18: ContentItem Publish Events
Task19: Indexing Foundation
Task20: ContentItem Published Index Projection
Task21: Cache Abstraction Foundation
Task22: Cache Invalidation Subscribers
Task23: ContentSearchService Foundation
Task24: myIR IndexWriter Adapter
Task25: OpenSearch IndexWriter Adapter
Task26: Embedding Index Projection
Notes

This ADR intentionally does not choose a concrete search backend.

Codex should be able to support multiple indexing and search mechanisms over time.

The immediate architectural decision is separation of responsibility, not infrastructure selection.