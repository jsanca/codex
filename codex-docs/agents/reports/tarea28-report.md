# Task 28 Post-Task Report — Move Indexing Classes to codex-index

## Status
Completed.

## Files Changed

### codex-index — new source files
- `src/main/java/codex/index/api/IndexDocumentId.java`
- `src/main/java/codex/index/api/IndexResourceType.java`
- `src/main/java/codex/index/api/IndexWriter.java`
- `src/main/java/codex/index/api/IndexDocument.java`
- `src/main/java/codex/index/internal/NoOpIndexWriter.java`
- `src/main/java/codex/index/internal/RecordingIndexWriter.java`
- `src/main/java/codex/index/internal/ContentItemProjectionSource.java`
- `src/main/java/codex/index/internal/RepositoryContentItemProjectionSource.java`
- `src/main/java/codex/index/internal/ContentItemIndexDocumentMapper.java`
- `src/main/java/codex/index/internal/ContentItemPublishedIndexingSubscriber.java`

### codex-index — new test files
- `src/test/java/codex/index/api/IndexDocumentIdTest.java`
- `src/test/java/codex/index/api/IndexResourceTypeTest.java`
- `src/test/java/codex/index/api/IndexDocumentTest.java`
- `src/test/java/codex/index/internal/NoOpIndexWriterTest.java`
- `src/test/java/codex/index/internal/RecordingIndexWriterTest.java`
- `src/test/java/codex/index/internal/ContentItemIndexDocumentMapperTest.java`
- `src/test/java/codex/index/internal/ContentItemPublishedIndexingSubscriberTest.java`
- `src/test/java/codex/index/internal/RepositoryContentItemProjectionSourceTest.java`
- `src/test/java/codex/index/internal/ContentItemIndexingIntegrationTest.java`

### codex-index — modified
- `pom.xml` — added `codex-codex`, `slf4j-api`, `slf4j-simple` (test) dependencies
- `src/main/java/module-info.java` — added `requires transitive codex.codex;`, `requires org.slf4j;`

### codex-index — deleted
- `src/main/java/codex/index/api/IndexApiMarker.java` (placeholder)
- `src/main/java/codex/index/internal/DefaultIndexModule.java` (placeholder)

### codex-codex — modified
- `src/main/java/module-info.java` — removed `exports codex.codex.api.index;`, added `exports codex.codex.internal.repository to codex.index;`
- `src/main/java/codex/codex/internal/runtime/CodexRuntime.java` — removed `inMemory(IndexWriter)` overload, removed indexing subscriber wiring; `inMemory()` now wires an empty `LocalCodexEventDispatcher`
- `src/test/java/codex/codex/internal/service/TestCodexContext.java` — removed `createWithIndexWriter(IndexWriter)`

### codex-codex — deleted
- `src/main/java/codex/codex/api/index/` (entire package: `IndexDocumentId`, `IndexResourceType`, `IndexWriter`, `IndexDocument`)
- `src/main/java/codex/codex/internal/index/` (entire package: `NoOpIndexWriter`, `RecordingIndexWriter`, `ContentItemProjectionSource`, `RepositoryContentItemProjectionSource`, `ContentItemIndexDocumentMapper`, `ContentItemPublishedIndexingSubscriber`)
- `src/test/java/codex/codex/api/index/` (entire package: `IndexDocumentIdTest`, `IndexDocumentTest`, `IndexResourceTypeTest`)
- `src/test/java/codex/codex/internal/index/` (entire package: all mapper/subscriber/writer/source tests)
- `src/test/java/codex/codex/internal/runtime/ContentItemIndexingPipelineIntegrationTest.java`

## Tests Run
`mvn test -pl codex-fundamentum,codex-codex,codex-index -am`

- codex-fundamentum: all pass
- codex-codex: all pass
- codex-index: 80 tests, 0 failures

## Architectural Decisions

**Qualified export**: `codex-codex` uses `exports codex.codex.internal.repository to codex.index` to grant `RepositoryContentItemProjectionSource` access to the repository interfaces without leaking them to all modules.

**`CodexRuntime` subscriber removal**: Indexing is no longer wired in `CodexRuntime`. The `inMemory()` method now composes an empty `LocalCodexEventDispatcher`. Future runtime assembly modules will compose the full pipeline across module boundaries.

**`ContentItemIndexingPipelineIntegrationTest` deleted**: This test verified the end-to-end pipeline through `CodexRuntime`. With indexing moved to `codex-index`, equivalent coverage now lives in `ContentItemIndexingIntegrationTest` in `codex-index`, which wires the pipeline manually without `CodexRuntime`.

**Stub repositories over Mockito**: Projection source tests use hand-written stub implementations. This is consistent with the rest of the codebase and avoids introducing Mockito as a test dependency.

## Follow-up Tasks
- A future runtime/assembly module will wire `ContentItemPublishedIndexingSubscriber` into a `LocalCodexEventDispatcher` with the `codex-index` components alongside `CodexRuntime`'s services.
- `RepositoryContentItemProjectionSource` can be extended with a cache layer once the cache foundation (`codex-fundamentum/api/cache`) is integrated into `codex-index`.
