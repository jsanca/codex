````markdown id="task36-index-runtime"
# Task36: IndexRuntime

## Objective

Create the first module runtime for `codex-index`.

`IndexRuntime` should assemble the internal indexing projection components and expose them through the generic runtime abstraction introduced in `codex-fundamentum`.

This task mirrors the pattern already introduced by `ChroniconRuntime`.

## Decision Context

Codex now has generic runtime abstractions in `codex-fundamentum`:

- `CodexModuleRuntime`
- `CodexModuleRuntimeProvider`
- `CodexRuntimeContext`
- `MapBackedCodexRuntimeContext`

Chronicon already has a module runtime:

```text
ChroniconRuntime
  -> ChroniconRepository
  -> Chronicon event subscribers
  -> CodexModuleRuntime
````

Index should now follow the same pattern:

```text
IndexRuntime
  -> IndexWriter
  -> ContentItemProjectionReader
  -> ReaderContentItemProjectionSource
  -> ContentItemIndexDocumentMapper
  -> ContentItemPublishedIndexingSubscriber
  -> CodexModuleRuntime
```

`codex-index` must not access `codex-codex` internal repositories.

It should receive the public core projection contract:

```text
ContentItemProjectionReader
```

from the composition layer.

Example future usage:

```java
var core = CodexRuntime.inMemory();

var index = IndexRuntime.inMemory(
        core.contentItemProjectionReader()
);

var localDispatcher = new LocalCodexEventDispatcher(index.subscribers());
```

This task should not create a global application runtime.

## Scope

Implement:

* `IndexRuntime`
* tests
* README update if useful

Do not implement:

* `ApplicationRuntime`
* `PortaRuntime`
* `CoreRuntime` rename
* `CodexModuleRuntimeProvider`
* ServiceLoader provider declarations
* Spring integration
* global runtime assembly
* dynamic registry
* OpenSearch writer
* Lucene writer
* myIR writer
* Elasticsearch writer
* embedding/vector writer
* `ContentSearchService`
* cache integration
* cache invalidation subscribers
* audit/Chronicon changes
* workflow
* REST/GraphQL

## Location

Create:

```text
codex-index/src/main/java/codex/index/internal/IndexRuntime.java
```

The class may stay internal for now.

Do not export `codex.index.internal`.

## 1. IndexRuntime Shape

`IndexRuntime` should implement:

```text
CodexModuleRuntime
```

Suggested API:

```java
public final class IndexRuntime implements CodexModuleRuntime {

    public static IndexRuntime inMemory(ContentItemProjectionReader projectionReader)

    public static IndexRuntime withWriter(
            ContentItemProjectionReader projectionReader,
            IndexWriter indexWriter
    )

    public IndexWriter indexWriter()

    @Override
    public String moduleName()

    @Override
    public List<CodexEventSubscriber<? extends CodexEvent>> subscribers()

    @Override
    public void close()
}
```

Requirements:

* `moduleName()` should return `"codex-index"`
* `subscribers()` should return an immutable list
* `indexWriter()` should expose the configured `IndexWriter`
* `close()` should be idempotent and no-op for now
* validate null `projectionReader`
* validate null `indexWriter`
* no ServiceLoader
* no Spring
* no global registry
* no dynamic subscriber registration

## 2. inMemory Factory

Implement:

```java
public static IndexRuntime inMemory(ContentItemProjectionReader projectionReader)
```

It should create:

```text
NoOpIndexWriter
ReaderContentItemProjectionSource
ContentItemIndexDocumentMapper
ContentItemPublishedIndexingSubscriber
```

Use `NoOpIndexWriter` as the default writer.

Reason:

Indexing should be structurally wired but not require external infrastructure.

## 3. withWriter Factory

Implement:

```java
public static IndexRuntime withWriter(
        ContentItemProjectionReader projectionReader,
        IndexWriter indexWriter
)
```

This should create the same subscriber list using the provided writer.

This is useful for tests with `RecordingIndexWriter`.

Do not introduce backend-specific writers.

## 4. Subscriber List

The runtime should expose current index subscribers:

* `ContentItemPublishedIndexingSubscriber`

Do not include unsupported/future subscribers.

Do not create generic subscriber discovery.

Do not introduce reflection or annotations.

## 5. Internal Assembly

`IndexRuntime` should internally assemble:

```text
ReaderContentItemProjectionSource
ContentItemIndexDocumentMapper
ContentItemPublishedIndexingSubscriber
```

Expected flow:

```text
ContentItemPublishedEvent
  -> ContentItemPublishedIndexingSubscriber
     -> ReaderContentItemProjectionSource
        -> ContentItemProjectionReader
     -> ContentItemIndexDocumentMapper
     -> IndexWriter
```

Do not use `RepositoryContentItemProjectionSource`.

Do not import `codex.codex.internal.repository`.

## 6. Tests

Add JUnit 5 tests.

Suggested test class:

```text
codex-index/src/test/java/codex/index/internal/IndexRuntimeTest.java
```

Test cases:

* `inMemory` creates runtime
* `moduleName` returns `codex-index`
* `indexWriter` is not null
* `inMemory` uses `NoOpIndexWriter`
* `subscribers` contains one subscriber
* `subscribers` snapshot is immutable
* `withWriter` rejects null projection reader
* `withWriter` rejects null index writer
* `withWriter` uses provided writer
* `close` does not throw
* `close` is idempotent
* runtime subscriber works with `LocalCodexEventDispatcher`

For dispatcher test:

1. Create a stub `ContentItemProjectionReader`.
2. Create a `RecordingIndexWriter`.
3. Create `IndexRuntime.withWriter(stubReader, recordingWriter)`.
4. Create `LocalCodexEventDispatcher(runtime.subscribers())`.
5. Dispatch a deterministic `ContentItemPublishedEvent`.
6. Assert one index document was upserted.

Use deterministic event data.

Do not require `CodexRuntime`.

Do not use repositories from `codex-codex.internal`.

## 7. Stub Projection Reader for Tests

Use a private test stub for `ContentItemProjectionReader`.

It should return:

* a deterministic `ContentItem`
* a deterministic `ContentRevision`

for the published event.

Do not use Mockito.

Do not use internal repositories.

## 8. Documentation

Update `codex-index/README.md` briefly if useful.

Mention:

* Index now has a module runtime.
* `IndexRuntime.inMemory(...)` creates a no-op writer and indexing subscriber.
* `IndexRuntime.withWriter(...)` allows tests or future adapters to provide a writer.
* runtime assembly with core/chronicon/porta is future work.
* ServiceLoader provider is future work.
* OpenSearch/Lucene/myIR/embedding adapters are future work.

Do not rewrite the README.

## 9. Module Info

No `module-info.java` changes should be needed if current dependencies already include:

```java
requires codex.fundamentum;
requires codex.codex;
```

If compilation requires changes, keep them minimal.

Do not export internal packages.

Do not add `provides` declarations.

## 10. Acceptance Criteria

Task is complete when:

* `IndexRuntime` exists
* it implements `CodexModuleRuntime`
* it exposes current index subscribers
* it can use either `NoOpIndexWriter` or a provided writer
* it depends on public `ContentItemProjectionReader`
* it does not import `codex.codex.internal.repository`
* tests prove runtime composition works with `LocalCodexEventDispatcher`
* no global runtime/assembly is introduced
* no ServiceLoader is introduced
* no Spring is introduced
* tests pass

## 11. Maven Commands

Run:

```bash
mvn test -pl codex-index -am
```

If practical, also run:

```bash
mvn test -pl codex-fundamentum,codex-codex,codex-index -am
```

Report command results.

## 12. Post-Task Report

After implementation, report:

* files created
* files modified
* tests added/updated
* Maven commands run
* whether tests passed
* intentional deviations
* open questions
* recommended follow-up tasks

## 13. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Keep runtime explicit and small.
* Do not create ServiceLoader provider.
* Do not create global runtime assembly.
* Do not introduce Service Locator.
* Do not wire into CodexRuntime.
* Do not create ChroniconRuntime changes.
* Do not create ApplicationRuntime.
* Do not introduce Spring.
* Do not introduce indexing backend adapters.
* Do not implement ContentSearchService.
* Do not implement cache.
* Do not implement audit changes.
* Do not implement workflow.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.

