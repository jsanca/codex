# Task14: ContentItem Event Publishing Pipeline

## Objective

Add event publishing support for `ContentItemService` using the same decorator-based pipeline already used for `SiteService` and `ContentTypeService`.

This task introduces the first content item event and an `EventPublishingContentItemService` decorator.

Events must be dispatched through `CodexEventDispatcher`, so they automatically benefit from the existing `DeferredEventDispatcher` and `TransactionContext` behavior:

- inside a transaction: events are buffered and dispatched after commit
- on rollback: events are discarded
- outside a transaction: events are dispatched immediately

Do not implement content revisions, workflow, publishing, audit, cache invalidation, search indexing, permissions, or external brokers in this task.

## Architectural direction

`CodexContentItemService` owns content item application semantics.

`EventPublishingContentItemService` owns event publication.

Do not inject `CodexEventDispatcher` directly into `CodexContentItemService`.

Use service decorators/wrappers.

Expected service chain:

`ContentItemService`
`  EventPublishingContentItemService`
`    -> CodexContentItemService`
`       -> MemoryContentItemRepository`

Event dispatch chain:

`EventPublishingContentItemService`
`  -> DeferredEventDispatcher`
`     -> actual CodexEventDispatcher delegate`

Events are facts that already happened.

Events should only be published after successful mutating operations.

Invalid operations must not publish events.

Rollback must discard deferred events.

## Scope

Implement:

- `ContentItemCreatedEvent`
- `ForwardingContentItemService`
- `EventPublishingContentItemService`
- unit tests for the event publishing decorator
- integration tests for content item events with `TransactionContext`
- runtime wiring in `CodexRuntime` if `ContentItemService` is already exposed there
- documentation note or ADR if appropriate

Do not implement:

- ContentRevision
- ContentItem publish/unpublish/archive operations
- ContentItemUpdatedEvent
- ContentItemPublishedEvent
- ContentItemArchivedEvent
- workflow
- audit subscribers
- cache invalidation subscribers
- search indexing subscribers
- event subscribers
- Kafka/pubsub/outbox
- Custos permissions
- Spring integration
- persistence
- TreeableResource
- CodexResourcePath
- JavaScript extensions

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.event.ContentItemCreatedEvent`
- `codex.codex.internal.service.ForwardingContentItemService`
- `codex.codex.internal.service.EventPublishingContentItemService`
- existing content item service tests
- existing content item integration tests or a new integration test class

## 1. Create ContentItemCreatedEvent

Create:

`codex.codex.api.model.event.ContentItemCreatedEvent`

Use a Java record.

The event should implement `CodexEvent`.

Fields:

- `ContentItemId id`
- `SiteKey siteKey`
- `ContentTypeKey contentTypeKey`
- `ContentTypeVersionId contentTypeVersionId`
- `ContentItemKey key`
- `Actor actor`
- `Instant occurredAt`

Required imports will likely include:

- `codex.codex.api.model.identity.ContentItemId`
- `codex.codex.api.model.identity.ContentItemKey`
- `codex.codex.api.model.identity.ContentTypeKey`
- `codex.codex.api.model.identity.ContentTypeVersionId`
- `codex.codex.api.model.identity.SiteKey`
- `codex.fundamentum.api.event.CodexEvent`
- `codex.fundamentum.api.model.Actor`
- `java.time.Instant`
- `java.util.Objects`

Validation requirements:

- all fields must be non-null
- use `Objects.requireNonNull`
- add JavaDoc

Example shape:

`public record ContentItemCreatedEvent(ContentItemId id, SiteKey siteKey, ContentTypeKey contentTypeKey, ContentTypeVersionId contentTypeVersionId, ContentItemKey key, Actor actor, Instant occurredAt) implements CodexEvent`

Do not include content values in the event.

Reason:

- values may be large
- values may be sensitive
- audit/search/cache subscribers can load the item by id if needed later

## 2. Create ForwardingContentItemService

Create:

`codex.codex.internal.service.ForwardingContentItemService`

This should mirror the pattern already used by:

- `ForwardingSiteService`
- `ForwardingContentTypeService`

Requirements:

- interface
- extends `ContentItemService`
- has one abstract method:

`ContentItemService getDelegate()`

- implements every `ContentItemService` method as a default method that forwards to `getDelegate()`
- no state
- no Spring
- internal package only
- JavaDoc explaining it is a forwarding helper for decorators

This allows decorators to override only the methods where they add behavior.

## 3. Create EventPublishingContentItemService

Create:

`codex.codex.internal.service.EventPublishingContentItemService`

Requirements:

- `final`
- implements `ForwardingContentItemService`
- constructor receives:
    - `ContentItemService delegate`
    - `CodexEventDispatcher eventDispatcher`
    - `Clock clock`
- validate constructor dependencies with `Objects.requireNonNull`
- implement `getDelegate()`
- override mutating methods:
    - `create`
- read-only methods should be forwarded by `ForwardingContentItemService`
- no Spring annotations
- no transaction annotations
- no persistence concerns

## 4. EventPublishingContentItemService behavior

### create

Behavior:

- require non-null command
- require non-null actor
- call delegate `create`
- dispatch `ContentItemCreatedEvent` after delegate returns successfully
- event must include:
    - content item id
    - siteKey
    - contentTypeKey
    - contentTypeVersionId
    - content item key
    - actor
    - `clock.instant()`
- return created content item

If delegate throws, do not dispatch an event.

## 5. Invalid operations

Invalid operations must not publish events.

Examples:

- duplicate content item key
- missing content type
- content type is `DRAFT`
- content type is `ARCHIVED`
- content type has no latest published version
- missing content type version
- content type version is not `PUBLISHED`
- unknown field value
- missing required field
- null command
- null actor

In all such cases, no `ContentItemCreatedEvent` should be dispatched.

## 6. Runtime wiring

If `CodexRuntime` already exposes `ContentItemService`, update its in-memory wiring so `contentItemService()` returns the outermost decorator:

`EventPublishingContentItemService`

Expected wiring:

`MemoryContentItemRepository`
`CodexContentItemService`
`EventPublishingContentItemService`

Use the existing runtime `DeferredEventDispatcher` and `Clock`.

Do not create a second independent event dispatcher for content items.

Do not create a second independent executor.

Site, ContentType, and ContentItem should share the same runtime event dispatcher.

If `CodexRuntime` does not expose `ContentItemService` yet, add it only if all content item pieces are already stable and existing tests pass.

## 7. Tests for events

Add plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer fake delegates or real in-memory services depending on what keeps tests simple and reliable.

### Event record tests

Add tests for:

- `ContentItemCreatedEvent` rejects null id
- `ContentItemCreatedEvent` rejects null siteKey
- `ContentItemCreatedEvent` rejects null contentTypeKey
- `ContentItemCreatedEvent` rejects null contentTypeVersionId
- `ContentItemCreatedEvent` rejects null key
- `ContentItemCreatedEvent` rejects null actor
- `ContentItemCreatedEvent` rejects null occurredAt

### ForwardingContentItemService tests

Add tests similar to existing forwarding service tests.

Verify that read-only and mutating methods forward to delegate by default.

Keep tests minimal.

### EventPublishingContentItemService tests

Add tests for:

- `create` delegates and publishes `ContentItemCreatedEvent`
- `create` does not publish when delegate throws
- required constructor arguments reject null
- required method arguments reject null

Verify event payload:

- id
- siteKey
- contentTypeKey
- contentTypeVersionId
- key
- actor
- occurredAt

## 8. Integration tests with TransactionContext

Add an integration test for the content item event pipeline using real production components.

No mocks.

No Spring.

No external infrastructure.

Use:

- `TransactionContext`
- `EventPublishingContentItemService`
- `CodexContentItemService`
- `MemoryContentItemRepository`
- `MemoryContentTypeRepository`
- `MemoryContentTypeVersionRepository`
- `DeferredEventDispatcher`
- recording dispatcher
- synchronous test executor
- fixed clock

Or reuse `CodexRuntime` if it already exposes:

- `ContentTypeService`
- `ContentItemService`
- recording dispatcher access for tests

Test setup should create:

1. a content type
2. at least one required field
3. activate the content type so version `1` is published
4. create content items against that published version

### create

1. `content item create inside transaction does not dispatch before commit`
    - inside `runInTransaction`, call create
    - before lambda returns assert zero events

2. `content item create inside transaction dispatches ContentItemCreatedEvent after commit`
    - call create inside transaction
    - after commit assert exactly one `ContentItemCreatedEvent`

3. `content item create rollback does not dispatch event`
    - inside transaction call create, then throw `RuntimeException`
    - after catching exception assert zero events

4. `content item outside transaction dispatches immediately`
    - call create outside any transaction
    - assert `ContentItemCreatedEvent` is dispatched immediately

### invalid create

5. `duplicate content item create does not dispatch event`
    - create item once
    - clear recorded events
    - attempt to create same item again
    - assert duplicate exception
    - assert zero events

6. `content item create with missing required field does not dispatch event`
    - attempt create without required field
    - assert validation exception
    - assert zero events

7. `content item create with unknown field does not dispatch event`
    - attempt create with unknown field
    - assert validation exception
    - assert zero events

8. `content item create for non active content type does not dispatch event`
    - use content type that is still `DRAFT` or `ARCHIVED`
    - assert semantic exception
    - assert zero events

## 9. Documentation / ADR

Add or update a short note explaining:

- content item events follow the same decorator pattern as site and content type events
- content item events are dispatched through `CodexEventDispatcher`
- `DeferredEventDispatcher` controls transaction-aware delivery
- invalid content item creation does not emit events
- rollback discards buffered content item events
- content values are intentionally not included in `ContentItemCreatedEvent`
- content item update/publish/archive events are future work
- content revisions are future work
- workflow events are future work

If creating an ADR, suggested title:

`ADR-006: ContentItem Event Publishing Pipeline`

Status may be `Draft` unless the code and tests are already accepted.

## Constraints

- Follow CLAUDE.md conventions.
- Follow the same style used by existing Site and ContentType event publishing.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No Custos implementation.
- No ContentRevision implementation.
- No workflow implementation.
- No content item update/publish/archive events.
- No content item field events.
- No audit.
- No cache invalidation.
- No search indexing.
- No TreeableResource implementation.
- No CodexResourcePath implementation.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable classes.
- Run the smallest relevant Maven test or compile command after implementation.