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

Skeleton only. No implementation has been migrated here yet.

The indexing foundation currently lives inside `codex-codex` during MVP. A future task will move
indexing abstractions and subscribers into this module once module boundaries are finalized.

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
