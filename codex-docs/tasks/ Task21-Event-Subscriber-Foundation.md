# Task21: Event Subscriber Foundation

## Objective

Introduce a small, explicit event subscriber abstraction for Codex.

Codex already has:

- `CodexEvent`
- `CodexEventDispatcher`
- `DeferredEventDispatcher`
- domain event publishing decorators
- first projection subscriber: `ContentItemPublishedIndexingSubscriber`

However, projection subscribers currently rely on ad-hoc `handle(...)` methods.

This task formalizes the subscriber contract so indexing, cache invalidation, audit, workflow continuation, and future projections can all use the same explicit event subscriber model.

## Architectural direction

Keep event subscription simple and explicit.

Do not introduce annotations yet.

Do not introduce reflection-based method scanning.

Do not introduce Spring events.

Do not introduce Kafka, pub/sub, outbox, or external broker integration.

Use an interface.

Expected future flow:

```text
EventPublishingContentItemService
  -> DeferredEventDispatcher
     -> LocalCodexEventDispatcher
        -> CodexEventSubscriber<ContentItemPublishedEvent>
           -> ContentItemPublishedIndexingSubscriber
              -> IndexWriter
```
DeferredEventDispatcher remains responsible for transaction-aware delivery.

LocalCodexEventDispatcher is responsible for delivering events to in-process subscribers.

Scope

Implement:

CodexEventSubscriber<E extends CodexEvent>
LocalCodexEventDispatcher
update ContentItemPublishedIndexingSubscriber to implement CodexEventSubscriber<ContentItemPublishedEvent>
tests

Do not implement:

annotation-based subscribers
reflection scanning
async subscriber execution
subscriber ordering
retry policies
dead-letter queues
outbox
Kafka/pubsub
Spring integration
event filtering DSL
event priority
workflow engine
audit system
cache invalidation system
OpenSearch/myIR/embedding integrations
Location

Use codex-fundamentum for generic event abstractions.

Suggested locations:

codex.fundamentum.api.event.CodexEventSubscriber
codex.fundamentum.api.event.LocalCodexEventDispatcher

Update:

codex.codex.internal.index.ContentItemPublishedIndexingSubscriber

If module exports are required, update module-info.java.

Do not place generic event abstractions inside codex-codex if they are reusable across modules.

1. Create CodexEventSubscriber

Create:

codex.fundamentum.api.event.CodexEventSubscriber

Interface shape:

CodexEventSubscriber<E extends CodexEvent>

Required methods:

Class<E> eventType()

void handle(E event)

Requirements:

generic over event type
eventType() must identify the event class handled by this subscriber
handle(...) should receive the strongly typed event
add JavaDoc
no default async behavior
no ordering
no retries
no checked exceptions

Example conceptual shape:

public interface CodexEventSubscriber<E extends CodexEvent> {
Class<E> eventType();
void handle(E event);
}
2. Create LocalCodexEventDispatcher

Create:

codex.fundamentum.api.event.LocalCodexEventDispatcher

This dispatcher delivers events to in-process subscribers.

It should implement:

CodexEventDispatcher

Constructor should accept:

Collection<? extends CodexEventSubscriber<? extends CodexEvent>> subscribers

Requirements:

validate subscribers collection non-null
validate no null subscriber entries
defensively copy subscribers
dispatch(CodexEvent event) validates event non-null
dispatch only to subscribers whose eventType().isInstance(event)
support multiple subscribers for the same event
if no subscriber matches, dispatch is a no-op
no async execution
no retry
no ordering guarantee beyond collection iteration order
no exception swallowing unless existing dispatcher conventions say otherwise

Exception behavior:

if a subscriber throws, let the exception propagate
do not call remaining subscribers after exception unless the implementation naturally already did before throwing
production retry/dead-letter behavior is future work

Add JavaDoc explaining this is an in-process local dispatcher.

3. Type-safe dispatch helper

Because Java generics require casting, keep the cast private and contained.

Suggested private helper:

private <E extends CodexEvent> void dispatchToSubscriber(CodexEventSubscriber<E> subscriber, CodexEvent event)

Use @SuppressWarnings("unchecked") only around the narrow cast.

Do not spread unchecked casts across the codebase.

4. Update ContentItemPublishedIndexingSubscriber

Update:

codex.codex.internal.index.ContentItemPublishedIndexingSubscriber

It should implement:

CodexEventSubscriber<ContentItemPublishedEvent>

Add:

@Override
public Class<ContentItemPublishedEvent> eventType() {
return ContentItemPublishedEvent.class;
}

Rename or keep handle(ContentItemPublishedEvent event) as the interface method.

The existing behavior should remain unchanged:

validate event non-null
load content item
load published revision
map to IndexDocument
call IndexWriter.upsert(...)
throw IllegalStateException if item or revision cannot be found

Do not add indexing policies in this task.

Do not add async behavior.

Do not add retries.

5. Runtime wiring

Do not change CodexRuntime unless there is already a clean place to wire subscribers.

For this task, it is acceptable to test LocalCodexEventDispatcher directly.

Future runtime work may wire:

DeferredEventDispatcher
-> LocalCodexEventDispatcher
-> subscribers

Do not force runtime subscriber registration into this task.

Do not introduce a service locator or global event bus.

6. Tests

Add plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

CodexEventSubscriber tests

Usually no direct tests are required beyond compile usage.

If desired, create a tiny test subscriber inside dispatcher tests.

LocalCodexEventDispatcher tests

Add tests for:

dispatch sends matching event to subscriber
dispatch does not send event to non-matching subscriber
dispatch sends event to multiple matching subscribers
dispatch with no matching subscribers is no-op
constructor rejects null subscribers collection
constructor rejects null subscriber entry
dispatch rejects null event
subscriber exception propagates
subscriber eventType must not be null if this is validated

Use simple test event records implementing CodexEvent.

Use simple recording subscribers.

ContentItemPublishedIndexingSubscriber tests

Update existing tests or add new assertions:

subscriber implements CodexEventSubscriber
eventType() returns ContentItemPublishedEvent.class
dispatcher can invoke subscriber through LocalCodexEventDispatcher
when dispatched through local dispatcher, published event results in one index upsert

Use:

MemoryContentItemRepository
MemoryContentRevisionRepository
RecordingIndexWriter
ContentItemIndexDocumentMapper
LocalCodexEventDispatcher

Do not require CodexRuntime for these tests.

7. Documentation

Add or update a short note explaining:

Codex now has an explicit local subscriber contract
event subscribers are in-process handlers
DeferredEventDispatcher controls transaction-aware delivery
LocalCodexEventDispatcher routes events to subscribers
subscribers are future extension points for:
indexing
cache invalidation
audit
workflow continuation
external broker publishing
annotation-based subscriber discovery is future work

If ADR-007 exists, optionally add a note that projection subscribers now implement CodexEventSubscriber.

8. Constraints
   Follow CLAUDE.md conventions.
   Keep the subscriber model explicit.
   No annotations.
   No reflection scanning.
   No Spring.
   No Kafka.
   No pub/sub infrastructure.
   No outbox.
   No async subscriber execution.
   No retry/dead-letter behavior.
   No subscriber priority.
   No workflow engine.
   No cache implementation.
   No audit implementation.
   No OpenSearch/myIR/embedding implementation.
   No broad refactors.
   Do not modify unrelated files.
   Do not modify .idea, target, build, or generated files.
   Keep comments and JavaDoc in English.
   Prefer small, explicit, testable classes.
   Run the smallest relevant Maven test command after implementation.