# Task40: Concilium End-to-End Publish Projection Smoke Test

## Objective

Add a focused end-to-end smoke test proving that `ConciliumRuntime` composes the core, index, and chronicon runtimes correctly.

The test should validate the full local in-memory flow:

```text
canonical content lifecycle
  -> domain event publishing
     -> ConciliumRuntime composed dispatcher
        -> IndexRuntime subscriber
           -> RecordingIndexWriter
        -> ChroniconRuntime subscriber
           -> RecordingChroniconRepository
````

This is a test-only task.

Do not introduce new runtime architecture.

Do not move `ConciliumRuntime`.

Do not introduce ServiceLoader, Spring, cache, search APIs, persistence, REST, or Porta integration.

## Decision Context

Codex now has:

```text
codex-codex
  -> CodexRuntime

codex-index
  -> IndexRuntime

codex-chronicon
  -> ChroniconRuntime

codex-concilium
  -> ConciliumRuntime
```

`ConciliumRuntime` is the first local runtime council that composes the core runtime and projection runtimes.

Before adding more features, we want one clear smoke test proving that:

1. canonical core operations emit domain events
2. Concilium routes those events to module subscribers
3. Index receives published content events and writes an index document
4. Chronicon receives published content events and writes an audit record

This test should become a confidence anchor for the architecture.

## Scope

Implement:

* one focused end-to-end integration/smoke test in `codex-concilium`
* use real in-memory core runtime
* use `RecordingIndexWriter`
* use `RecordingChroniconRepository`
* use real `IndexRuntime`
* use real `ChroniconRuntime`
* use real `ConciliumRuntime`
* perform a real content authoring/publish flow if existing services allow it

Do not implement:

* new production classes
* new runtime abstractions
* ServiceLoader discovery
* Spring
* Service Locator
* Porta integration
* REST/GraphQL
* persistence
* cache
* ContentSearchService
* audit query service
* timeline query service
* workflow
* AI/agent behavior
* external index backends

## Location

Create or update:

```text
codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeEndToEndTest.java
```

If existing test package conventions differ, follow the current `codex-concilium` test style.

This test may remain in an internal test package for now.

## 1. Test Name

Create a test named:

```text
publishing content item projects to index and chronicon
```

or Java-style:

```java
publishingContentItemProjectsToIndexAndChronicon()
```

## 2. Runtime Setup

The test should build the composed runtime explicitly.

Suggested shape:

```java
final CodexRuntime core = CodexRuntime.inMemory(/* dispatcher if required */);

final RecordingIndexWriter indexWriter = new RecordingIndexWriter();
final IndexRuntime index = IndexRuntime.withWriter(
        core.contentItemProjectionReader(),
        indexWriter
);

final RecordingChroniconRepository chroniconRepository = new RecordingChroniconRepository();
final ChroniconRuntime chronicon = ChroniconRuntime.withRepository(chroniconRepository);

final ConciliumRuntime concilium = ConciliumRuntime.compose(core, index, chronicon);
```

If the current `ConciliumRuntime.inMemory()` cannot expose a recording index writer / recording chronicon repository, prefer `compose(...)` with custom child runtimes.

Do not modify production code unless a very small test-support accessor is already consistent with existing runtime style.

## 3. Event Pipeline Expectation

The intended event path is:

```text
core service mutation
  -> EventPublishing*Service
     -> DeferredEventDispatcher
        -> core EventRecorder
        -> LocalCodexEventDispatcher(module subscribers)
           -> ContentItemPublishedIndexingSubscriber
           -> ContentItemPublishedChroniconSubscriber
```

The test should prove the publish event reaches both projection modules.

## 4. Content Flow

Use the real core services exposed by `CodexRuntime`.

Recommended flow:

```text
1. create site
2. create content type
3. add at least one field, preferably title
4. activate content type
5. create content item with title/body-like values
6. publish content item
```

Use existing command classes and service APIs.

Use deterministic values:

* fixed `SiteKey`
* fixed `ContentTypeKey`
* fixed `ContentItemKey`
* fixed `Actor`
* fixed `Clock` if available through runtime/test helpers
* stable title string

Do not use random UUIDs or `Instant.now()` in assertions unless unavoidable.

If the existing runtime does not expose a fixed clock, avoid asserting exact timestamps unless already available from returned objects/events.

## 5. Transaction Behavior

If publishing normally runs inside `TransactionContext`, preserve the existing pattern.

The test should assert projection results **after commit**, not before.

If there is a known helper for transaction execution, use it.

If current service methods dispatch immediately outside transactions, the test may execute without explicit transaction wrapping.

Do not change transaction infrastructure.

## 6. Index Assertions

After publishing, assert:

* `RecordingIndexWriter` received exactly one upsert for the published content item event
* indexed document resource type is `CONTENT_ITEM`
* indexed document id is deterministic and contains:

    * site key
    * content type key
    * content item key
* indexed document title matches the published title if title mapping is already implemented
* indexed document metadata contains:

    * content item id
    * content item key
    * content type key
    * content type version id
    * content revision id / published revision id if available

Do not over-assert implementation details that are not stable.

## 7. Chronicon Assertions

After publishing, assert:

* `RecordingChroniconRepository` saved at least one audit record for the publish event
* there is an audit record with:

    * `AuditAction.PUBLISHED`
    * subject type `content-item`
    * subject key matching the content item key
    * actor id matching the publishing actor id
    * metadata containing published revision id if current subscriber records it

It is okay if Chronicon also records earlier events from the setup flow, such as:

* site created
* content type created

In that case, filter for the `PUBLISHED` content-item audit record rather than asserting total saved records is exactly one.

## 8. Core Event Recorder Assertions

If `CodexRuntime` exposes recorded events, assert that a `ContentItemPublishedEvent` was recorded.

If accessing the event recorder is awkward or internal, do not force production API changes just for this test.

Projection assertions from Index and Chronicon are the most important acceptance criteria.

## 9. Avoid Brittle Assertions

Do not assert exact order across unrelated subscribers unless the dispatcher/repository explicitly guarantees order.

Do not assert full summary text word-for-word unless summaries are considered stable.

Prefer:

```text
summary is non-blank
metadata contains expected keys
subject/action/actor are correct
```

## 10. Optional Test: Concilium inMemory Creates Runtime

If not already covered, add or keep a small test proving:

```java
ConciliumRuntime.inMemory()
```

creates a composed runtime with:

* core runtime
* index runtime
* chronicon runtime
* non-empty subscribers
* non-null event dispatcher

Do not overbuild.

## 11. Documentation

Update `codex-concilium/README.md` if useful.

Add a short note:

```text
Concilium has an end-to-end smoke test proving that publishing content through the core can project to both Index and Chronicon through the composed runtime.
```

Do not rewrite the README.

## 12. Acceptance Criteria

Task is complete when:

* a Concilium end-to-end smoke test exists
* the test uses `ConciliumRuntime`
* the test performs a real content item publish flow or the closest existing supported equivalent
* index projection is verified through `RecordingIndexWriter`
* chronicon projection is verified through `RecordingChroniconRepository`
* no production behavior is changed unless strictly necessary
* no ServiceLoader is introduced
* no Spring is introduced
* no cache/search/persistence/REST/workflow is introduced
* tests pass

## 13. Maven Commands

Run:

```bash
mvn test -pl codex-concilium -am
```

If practical, also run:

```bash
mvn test -pl codex-fundamentum,codex-codex,codex-index,codex-chronicon,codex-concilium -am
```

Then run:

```bash
mvn compile
```

Report command results.

## 14. Post-Task Report

After implementation, report:

* files created
* files modified
* tests added/updated
* Maven commands run
* whether tests passed
* whether any production code changed
* what flow the end-to-end test performs
* what index assertions are made
* what chronicon assertions are made
* intentional deviations
* open questions
* recommended follow-up tasks

## 15. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Test-only task unless a tiny production change is strictly required.
* Do not introduce ServiceLoader.
* Do not introduce Spring.
* Do not introduce Service Locator.
* Do not move ConciliumRuntime.
* Do not add Porta integration.
* Do not add REST/GraphQL.
* Do not add persistence.
* Do not add cache.
* Do not add ContentSearchService.
* Do not add workflow.
* Do not add AI/agent behavior.
* Do not change domain semantics.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.
* Prefer one clear, meaningful smoke test over many brittle tests.

