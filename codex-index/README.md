# codex-index

> The Index makes knowledge discoverable.

## Responsibility

`codex-index` owns the search and indexing layer of the Codex CMS. It will eventually hold:

- Indexing abstractions: `IndexDocument`, `IndexDocumentId`, `IndexResourceType`, `IndexWriter`
- Search services and query abstractions
- Indexing projection subscribers (mapping domain events into index documents)
- Ranking and retrieval strategies
- Future adapters: Lucene, OpenSearch, Elasticsearch, myIR, vector indexes, hybrid search systems

## Current Status

Indexing foundation implemented (earlier tasks):

- `IndexDocument`, `IndexDocumentId`, `IndexResourceType`, `IndexWriter` — core indexing model
- `ContentItemIndexDocumentMapper` — maps a published item + revision into an `IndexDocument`
- `ContentItemPublishedIndexingSubscriber` — reacts to `ContentItemPublishedEvent` and writes to `IndexWriter`
- `ReaderContentItemProjectionSource` — adapts the public `ContentItemProjectionReader` contract for index use
- `NoOpIndexWriter` — discards all operations; suitable when indexing is not yet configured
- `RecordingIndexWriter` — records all operations for inspection in tests

Module runtime implemented (Task 36):

- `IndexRuntime` — assembles the writer, projection source, mapper, and subscriber into a single
  composition root implementing `CodexModuleRuntime` from `codex-fundamentum`.

Usage:

```java
// No-op writer: structurally wired, no infrastructure required
IndexRuntime runtime = IndexRuntime.inMemory(core.contentItemProjectionReader());
dispatcher.registerAll(runtime.subscribers());

// Custom writer: future adapters or RecordingIndexWriter in integration tests
IndexRuntime runtime = IndexRuntime.withWriter(projectionReader, recordingWriter);
```

**Not yet implemented:**

- Assembly with `CoreRuntime` / `ChroniconRuntime` into a global `ApplicationRuntime`
- `IndexRuntimeProvider` (ServiceLoader-based provider declaration)
- OpenSearch, Lucene, Elasticsearch, myIR, and vector index adapters
- `ContentSearchService` and query abstractions
- Cache invalidation subscribers


## Dependency Rules

- Does **not** own canonical content.
- Receives projections derived from domain events.
- May depend on `codex-codex` and `codex-fundamentum`.
- `codex-codex` must **never** depend on `codex-index`.
- Canonical services must not call indexing directly.
- Public content indexing should be driven by published-content events.

## Future Adapters

Lucene, OpenSearch, Elasticsearch, myIR, vector indexes, and hybrid search are planned as future
adapters. None are implemented in this task.
