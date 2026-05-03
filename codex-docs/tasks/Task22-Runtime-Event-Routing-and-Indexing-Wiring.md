# Task22: Runtime Event Routing and Indexing Wiring

## Objective

Wire the local event subscriber infrastructure into `CodexRuntime` without changing the existing `CodexEventDispatcher` interface.

Codex now has:

- `CodexEvent`
- `CodexEventDispatcher`
- `DeferredEventDispatcher`
- `LocalCodexEventDispatcher`
- `CodexEventSubscriber`
- `ContentItemPublishedIndexingSubscriber`
- `IndexWriter`
- `NoOpIndexWriter`
- `RecordingIndexWriter`

This task closes the event projection loop:

```text
publish content item
  -> ContentItemPublishedEvent
     -> DeferredEventDispatcher
        -> CompositeCodexEventDispatcher
           -> recording dispatcher
           -> LocalCodexEventDispatcher
              -> ContentItemPublishedIndexingSubscriber
                 -> IndexWriter.upsert(IndexDocument)
```

The goal is to keep the dispatcher interface stable while allowing multiple dispatch targets internally.

Architectural direction

Use composition.

Do not change CodexEventDispatcher.

Do not make subscribers dynamic yet.

Do not introduce Spring, reflection, annotations, Kafka, outbox, async subscriber execution, retry, or dead-letter behavior.

Introduce a small composite dispatcher:

CompositeCodexEventDispatcher
-> delegates to multiple CodexEventDispatcher instances

This keeps the existing interface intact.

No caller should need to know whether the dispatcher is recording, routing to subscribers, publishing externally, or doing multiple things.

Scope

Implement:

CompositeCodexEventDispatcher
runtime wiring for LocalCodexEventDispatcher
runtime wiring for ContentItemPublishedIndexingSubscriber
default NoOpIndexWriter in runtime
optional test/runtime support for RecordingIndexWriter
integration tests proving publish event routes to indexing subscriber

Do not implement:

dynamic subscriber registry
annotation-based subscribers
reflection scanning
Spring integration
Kafka/pubsub/outbox
async subscribers
subscriber ordering framework
retry/dead-letter behavior
OpenSearch writer
myIR writer
Lucene writer
embeddings/vector writer
cache invalidation
audit subscriber
workflow subscriber
search service
REST
persistence
Location

Suggested locations:

codex.fundamentum.api.event.CompositeCodexEventDispatcher
codex.codex.internal.runtime.CodexRuntime
existing runtime tests
existing indexing tests

If CompositeCodexEventDispatcher is considered reusable across modules, keep it in codex-fundamentum.

If module exports are required, update module-info.java.

Do not place generic event infrastructure inside codex-codex if it belongs in fundamentum.

1. Create CompositeCodexEventDispatcher

Create:

codex.fundamentum.api.event.CompositeCodexEventDispatcher

It should implement:

CodexEventDispatcher

Constructor:

CompositeCodexEventDispatcher(Collection<? extends CodexEventDispatcher> dispatchers)

Requirements:

validate dispatchers collection non-null
validate no null dispatcher entries
defensively copy dispatchers
dispatch(CodexEvent event) validates event non-null
dispatch event to every delegate dispatcher in collection iteration order
if no dispatchers exist, dispatch is a no-op
if a dispatcher throws, propagate the exception immediately
do not swallow exceptions
do not retry
do not continue after an exception unless natural loop behavior already reached previous delegates
no async execution
no transaction awareness here

JavaDoc should explain:

this is a simple fan-out dispatcher
it preserves the existing CodexEventDispatcher abstraction
it is useful for combining recording, local subscribers, external broker publishing, audit, or other dispatch targets
transaction-aware delivery remains the responsibility of DeferredEventDispatcher
2. Runtime event pipeline

Update CodexRuntime.inMemory() or the current runtime factory so event dispatching uses this shape:

DeferredEventDispatcher
-> CompositeCodexEventDispatcher
-> recording dispatcher if present
-> LocalCodexEventDispatcher
-> subscribers

Important:

DeferredEventDispatcher should remain the outer transaction-aware dispatcher.

The composite dispatcher should be the delegate inside DeferredEventDispatcher.

Do not make LocalCodexEventDispatcher transaction-aware.

Do not make CompositeCodexEventDispatcher transaction-aware.

3. Runtime subscribers

Create runtime subscribers during runtime construction.

For now, include:

ContentItemPublishedIndexingSubscriber

The subscriber should use:

ContentItemRepository
ContentRevisionRepository
IndexWriter
ContentItemIndexDocumentMapper

Use the same canonical repositories already used by ContentItemService.

Do not create duplicate repositories for the subscriber.

4. Default IndexWriter

By default, runtime should use:

NoOpIndexWriter

This means indexing is enabled structurally but does nothing unless a different writer is provided.

This is useful because:

the runtime can wire the subscriber safely
no external infrastructure is required
tests can override with RecordingIndexWriter
future OpenSearch/myIR/embedding writers can be plugged in later

Do not introduce OpenSearch, myIR, Lucene, or embeddings in this task.

5. Runtime customization for tests

If there is already a test runtime fixture, update it to allow a RecordingIndexWriter.

Possible approaches:

Option A: Runtime factory overload

Add a package-private or test-friendly factory if consistent with current style:

CodexRuntime.inMemory(IndexWriter indexWriter)

or:

CodexRuntime.inMemoryForTesting(IndexWriter indexWriter)
Option B: Runtime builder

Only if a builder already exists or is very small:

CodexRuntime.builder()
.indexWriter(recordingIndexWriter)
.buildInMemory()
Option C: TestCodexContext

If TestCodexContext exists and already wraps runtime creation, add support there:

TestCodexContext.createWithIndexWriter(RecordingIndexWriter writer)

Prefer the smallest change consistent with the existing test style.

Do not expose internal repositories publicly only to test indexing.

Do not create a large runtime builder unless the project already needs one.

6. Preserve event recording

If tests currently rely on recorded domain events, keep that behavior.

If there is currently a recording dispatcher in runtime tests, ensure it still receives events.

Expected shape:

CompositeCodexEventDispatcher
-> RecordingCodexEventDispatcher
-> LocalCodexEventDispatcher

This allows tests to assert:

event was recorded
subscriber also handled event
indexing writer received an upsert

Do not replace recording with local subscribers unless tests are updated intentionally.

7. Local dispatcher subscriber list

LocalCodexEventDispatcher should receive a fixed list of subscribers at runtime construction time.

For this task, dynamic subscriber registration is not required.

Document or preserve the decision:

Subscribers are fixed at dispatcher construction time.
Dynamic subscriber registration is future work.
8. Integration test: publish routes to index writer

Add a JUnit 5 integration test using real components.

Suggested test name:

publishing content item routes event to indexing subscriber

Flow:

create runtime/test context with RecordingIndexWriter
create site
create content type
add required title field
activate content type
create content item with title
clear recorded domain events if needed
clear recording index writer
publish content item inside TransactionContext.runInTransaction(...)
inside transaction, assert no index upsert happened yet if event dispatch is deferred
after commit:
assert ContentItemPublishedEvent was recorded
assert RecordingIndexWriter has exactly one upsert
assert upserted document resource type is CONTENT_ITEM
assert upserted document title is the published title
assert upserted document id is deterministic

This test proves:

publish -> event -> deferred dispatch -> local subscriber -> index writer
9. Integration test: rollback does not index

Add test:

publishing content item rollback does not index

Flow:

create runtime/test context with RecordingIndexWriter
create site
create content type
add required field
activate content type
create content item
clear recorded events and index writer
inside TransactionContext.runInTransaction(...):
publish content item
throw RuntimeException("forced rollback")
after catching exception:
assert no ContentItemPublishedEvent was recorded after rollback
assert RecordingIndexWriter has no upserts

Note:

This validates event/index rollback behavior.

Do not assert repository data rollback unless repository transactions exist.

10. Integration test: NoOpIndexWriter does not fail

Add test:

runtime with default no-op index writer can publish content item

Flow:

create standard CodexRuntime.inMemory()
create site
create content type
add field
activate content type
create content item
publish content item
assert publish succeeds

This ensures indexing wiring does not require external infrastructure.

11. Composite dispatcher tests

Add plain unit tests for CompositeCodexEventDispatcher.

Test cases:

dispatch sends event to all delegate dispatchers
dispatch with empty delegates is no-op
constructor rejects null collection
constructor rejects null dispatcher entry
dispatch rejects null event
exception from delegate propagates
delegates after throwing delegate are not called if loop stops naturally

Use small recording dispatchers in the test.

No Mockito required.

12. Event pipeline ordering note

CompositeCodexEventDispatcher dispatches to delegates in collection iteration order.

For tests, if both recording and local subscriber are present, either order is acceptable unless tests rely on side effects.

Recommended runtime order:

Recording dispatcher first
Local subscriber dispatcher second

Reason:

event is recorded even if a subscriber fails
subscriber failure still propagates after recording

If this order is chosen, document it in runtime code comments if helpful.

Do not introduce a formal ordering framework.

13. Failure behavior

If indexing subscriber fails, the exception should propagate through:

LocalCodexEventDispatcher
CompositeCodexEventDispatcher
DeferredEventDispatcher

For now, this is acceptable.

Future work may introduce:

retry
dead-letter queue
projection failure isolation
async subscribers
outbox

Do not implement those in this task.

14. Documentation

Add or update a short note explaining:

CompositeCodexEventDispatcher allows multiple dispatch targets behind the same interface
LocalCodexEventDispatcher routes to in-process subscribers
DeferredEventDispatcher remains transaction-aware
indexing is now structurally wired into runtime through a subscriber
default indexing uses NoOpIndexWriter
tests can use RecordingIndexWriter
dynamic subscriber registration is future work

If ADR-007 exists, optionally update it to mention:

publish -> ContentItemPublishedEvent -> LocalCodexEventDispatcher -> ContentItemPublishedIndexingSubscriber -> IndexWriter
15. Constraints
    Follow CLAUDE.md conventions.
    Keep CodexEventDispatcher unchanged.
    Use composition.
    Keep subscribers fixed at runtime construction time.
    Do not implement dynamic registration.
    Do not introduce Spring.
    Do not introduce annotations.
    Do not introduce reflection scanning.
    Do not introduce Kafka/pubsub/outbox.
    Do not introduce async subscribers.
    Do not introduce retry/dead-letter behavior.
    Do not introduce OpenSearch/myIR/Lucene/embeddings.
    Do not introduce cache.
    Do not introduce audit.
    Do not introduce workflow.
    Do not introduce search API.
    No REST controllers.
    No persistence framework.
    No broad refactors.
    Do not modify unrelated files.
    Do not modify .idea, target, build, or generated files.
    Keep comments and JavaDoc in English.
    Prefer small, explicit, testable classes.
    Run the smallest relevant Maven test command after implementation.