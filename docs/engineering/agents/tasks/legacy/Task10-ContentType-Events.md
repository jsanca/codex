# Task10: ContentType Event Publishing Pipeline

## Objective

Add event publishing support for `ContentTypeService` using the same decorator-based pipeline already used for `SiteService`.

This task should introduce content type events and an `EventPublishingContentTypeService` decorator.

Events must be dispatched through `CodexEventDispatcher`, so they automatically benefit from the existing `DeferredEventDispatcher` and `TransactionContext` behavior:

- inside a transaction: events are buffered and dispatched after commit
- on rollback: events are discarded
- outside a transaction: events are dispatched immediately

Do not implement audit, cache invalidation, workflow continuation, permissions, Custos, versioning, content items, or field/schema events in this task.

## Architectural direction

`CodexContentTypeService` owns content type application semantics.

`EventPublishingContentTypeService` owns event publication.

Do not inject `CodexEventDispatcher` directly into `CodexContentTypeService`.

Use service decorators/wrappers.

Expected chain:

`ContentTypeService`
`  EventPublishingContentTypeService`
`    -> CodexContentTypeService`
`       -> MemoryContentTypeRepository`

Event dispatch chain:

`EventPublishingContentTypeService`
`  -> DeferredEventDispatcher`
`     -> actual CodexEventDispatcher delegate`

Events are facts that already happened.

Events should only be published after a successful mutating operation.

Idempotent operations must not publish events.

Invalid operations must not publish events.

Rollback must discard deferred events.

## Scope

Implement:

- `ContentTypeCreatedEvent`
- `ContentTypeActivatedEvent`
- `ContentTypeArchivedEvent`
- `ForwardingContentTypeService`
- `EventPublishingContentTypeService`
- unit tests for the event publishing decorator
- integration tests for content type events with `TransactionContext`
- runtime wiring in `CodexRuntime` if `ContentTypeService` is already exposed there

Do not implement:

- content type field events
- content type version events
- content item events
- event subscribers
- audit subscribers
- cache invalidation subscribers
- workflow subscribers
- Custos permissions
- Spring integration
- Kafka/pubsub/outbox
- TreeableResource
- CodexResourcePath
- JavaScript extensions

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.event.ContentTypeCreatedEvent`
- `codex.codex.api.model.event.ContentTypeActivatedEvent`
- `codex.codex.api.model.event.ContentTypeArchivedEvent`
- `codex.codex.internal.service.ForwardingContentTypeService`
- `codex.codex.internal.service.EventPublishingContentTypeService`
- existing content type service tests
- existing content type pipeline integration tests or a new integration test class

## 1. Create ContentType events

Create three events:

- `ContentTypeCreatedEvent`
- `ContentTypeActivatedEvent`
- `ContentTypeArchivedEvent`

Each event should implement `CodexEvent`.

Use Java records.

Each event should include:

- `ContentTypeId id`
- `SiteKey siteKey`
- `ContentTypeKey key`
- `Actor actor`
- `Instant occurredAt`

For activated and archived events, also include:

- `ContentTypeStatus previousStatus`
- `ContentTypeStatus newStatus`

Required imports will likely include:

- `codex.codex.api.model.identity.ContentTypeId`
- `codex.codex.api.model.identity.ContentTypeKey`
- `codex.codex.api.model.identity.SiteKey`
- `codex.codex.api.model.value.ContentTypeStatus`
- `codex.fundamentum.api.event.CodexEvent`
- `codex.fundamentum.api.model.Actor`
- `java.time.Instant`
- `java.util.Objects`

Validation requirements:

- all fields must be non-null
- use `Objects.requireNonNull`
- do not allow null statuses in status change events

Example event shape:

`public record ContentTypeCreatedEvent(ContentTypeId id, SiteKey siteKey, ContentTypeKey key, Actor actor, Instant occurredAt) implements CodexEvent`

`public record ContentTypeActivatedEvent(ContentTypeId id, SiteKey siteKey, ContentTypeKey key, ContentTypeStatus previousStatus, ContentTypeStatus newStatus, Actor actor, Instant occurredAt) implements CodexEvent`

`public record ContentTypeArchivedEvent(ContentTypeId id, SiteKey siteKey, ContentTypeKey key, ContentTypeStatus previousStatus, ContentTypeStatus newStatus, Actor actor, Instant occurredAt) implements CodexEvent`

Add JavaDoc to each event.

## 2. Create ForwardingContentTypeService

Create:

`codex.codex.internal.service.ForwardingContentTypeService`

This should mirror the pattern already used by `ForwardingSiteService`.

Requirements:

- interface
- extends `ContentTypeService`
- has one abstract method:

`ContentTypeService getDelegate()`

- implements every `ContentTypeService` method as a default method that forwards to `getDelegate()`
- no state
- no Spring
- internal package only
- JavaDoc explaining it is a forwarding helper for decorators

This allows decorators to override only the methods where they add behavior.

## 3. Create EventPublishingContentTypeService

Create:

`codex.codex.internal.service.EventPublishingContentTypeService`

Requirements:

- `final`
- implements `ForwardingContentTypeService`
- constructor receives:
    - `ContentTypeService delegate`
    - `CodexEventDispatcher eventDispatcher`
    - `Clock clock`
- validate constructor dependencies with `Objects.requireNonNull`
- implement `getDelegate()`
- override mutating methods:
    - `create`
    - `activate`
    - `archive`
- read-only methods should be forwarded by `ForwardingContentTypeService`
- no Spring annotations
- no transaction annotations
- no persistence concerns

## 4. EventPublishingContentTypeService behavior

### create

Behavior:

- require non-null command
- require non-null actor
- call delegate `create`
- dispatch `ContentTypeCreatedEvent` after delegate returns successfully
- event must include:
    - content type id
    - siteKey
    - content type key
    - actor
    - `clock.instant()`
- return created content type

If delegate throws, do not dispatch an event.

### activate

Behavior:

- require non-null command
- require non-null actor
- read current content type before delegating, using delegate `findByKey`
- call delegate `activate`
- if previous status equals resulting status, do not dispatch event
- if status changed, dispatch `ContentTypeActivatedEvent`
- event must include:
    - content type id
    - siteKey
    - content type key
    - previous status
    - new status
    - actor
    - `clock.instant()`
- return activated content type

If delegate throws, do not dispatch an event.

### archive

Behavior:

- require non-null command
- require non-null actor
- read current content type before delegating, using delegate `findByKey`
- call delegate `archive`
- if previous status equals resulting status, do not dispatch event
- if status changed, dispatch `ContentTypeArchivedEvent`
- event must include:
    - content type id
    - siteKey
    - content type key
    - previous status
    - new status
    - actor
    - `clock.instant()`
- return archived content type

If delegate throws, do not dispatch an event.

## 5. Missing previous state handling

For `activate` and `archive`, the decorator needs the previous status.

Use the existing service lookup before calling the delegate.

Expected pattern:

- `delegate.findByKey(command.siteKey(), command.key(), actor)`
- if missing, let delegate throw the authoritative `NotFoundException`
- do not create a different exception in the decorator
- if previous state is missing, call delegate and let it fail normally
- only dispatch when both previous state and resulting state are available and status changed

Do not duplicate business rules in the decorator.

The decorator observes before/after state only to decide whether an event should be published.

## 6. Idempotency

Idempotent operations must not publish events.

Examples:

- activating an already `ACTIVE` content type must not publish `ContentTypeActivatedEvent`
- archiving an already `ARCHIVED` content type must not publish `ContentTypeArchivedEvent`

The decorator should compare previous status and resulting status.

If they are equal, no event should be dispatched.

## 7. Invalid operations

Invalid operations must not publish events.

Examples:

- activating an `ARCHIVED` content type should throw `InvalidContentTypeStatusTransitionException`
- activating a missing content type should throw `NotFoundException`
- archiving a missing content type should throw `NotFoundException`

In all such cases, no content type event should be dispatched.

## 8. Runtime wiring

If `CodexRuntime` already exposes `ContentTypeService`, update its in-memory wiring so `contentTypeService()` returns the outermost decorator:

`EventPublishingContentTypeService`

Expected wiring:

`MemoryContentTypeRepository`
`CodexContentTypeService`
`EventPublishingContentTypeService`

Use the existing runtime `DeferredEventDispatcher` and `Clock`.

Do not create a second independent event dispatcher for content types.

Do not create a second independent executor.

Site and ContentType should share the same runtime event dispatcher.

If `CodexRuntime` does not expose `ContentTypeService` yet, add it only if all content type pieces are already stable and existing tests pass.

## 9. Tests for events

Add plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer fake delegates or real in-memory services depending on what keeps the tests simple and reliable.

### Event record tests

Add tests for:

- `ContentTypeCreatedEvent` rejects null id
- `ContentTypeCreatedEvent` rejects null siteKey
- `ContentTypeCreatedEvent` rejects null key
- `ContentTypeCreatedEvent` rejects null actor
- `ContentTypeCreatedEvent` rejects null occurredAt
- `ContentTypeActivatedEvent` rejects null previousStatus
- `ContentTypeActivatedEvent` rejects null newStatus
- `ContentTypeArchivedEvent` rejects null previousStatus
- `ContentTypeArchivedEvent` rejects null newStatus

### ForwardingContentTypeService tests

Add tests similar to `ForwardingSiteService` if such tests exist.

Verify that read-only and mutating methods forward to delegate by default.

Keep tests minimal.

### EventPublishingContentTypeService tests

Add tests for:

- `create` delegates and publishes `ContentTypeCreatedEvent`
- `create` does not publish when delegate throws
- `activate` publishes `ContentTypeActivatedEvent` when status changes from `DRAFT` to `ACTIVE`
- `activate` does not publish when already `ACTIVE`
- `activate` does not publish when delegate throws
- `archive` publishes `ContentTypeArchivedEvent` when status changes from `DRAFT` to `ARCHIVED`
- `archive` publishes `ContentTypeArchivedEvent` when status changes from `ACTIVE` to `ARCHIVED`
- `archive` does not publish when already `ARCHIVED`
- `archive` does not publish when delegate throws
- required constructor arguments reject null
- required method arguments reject null

Verify event payload:

- id
- siteKey
- key
- actor
- occurredAt
- previousStatus and newStatus for status events

## 10. Integration tests with TransactionContext

Add an integration test for the content type event pipeline using real production components.

No mocks.

No Spring.

No external infrastructure.

Use:

- `TransactionContext`
- `EventPublishingContentTypeService`
- `CodexContentTypeService`
- `MemoryContentTypeRepository`
- `DeferredEventDispatcher`
- recording dispatcher
- synchronous test executor
- fixed clock

Or reuse `CodexRuntime` if it already exposes `ContentTypeService` and a recording dispatcher.

Test cases:

### create

1. `content type create inside transaction does not dispatch before commit`
    - inside `runInTransaction`, call create
    - before lambda returns assert zero events

2. `content type create inside transaction dispatches ContentTypeCreatedEvent after commit`
    - call create inside transaction
    - after commit assert exactly one `ContentTypeCreatedEvent`

3. `content type create rollback does not dispatch event`
    - inside transaction call create, then throw `RuntimeException`
    - after catching exception assert zero events

### activate

4. `content type activate dispatches ContentTypeActivatedEvent only when status changes`
    - create DRAFT content type
    - activate in a new transaction
    - assert exactly one `ContentTypeActivatedEvent`

5. `idempotent content type activate does not dispatch event`
    - create and activate content type
    - call activate again
    - assert zero new events

6. `rollback after content type activate does not dispatch event`
    - create DRAFT content type
    - inside new transaction call activate, then throw `RuntimeException`
    - assert zero `ContentTypeActivatedEvent`

### archive

7. `content type archive dispatches ContentTypeArchivedEvent only when status changes`
    - create DRAFT content type
    - archive in a new transaction
    - assert exactly one `ContentTypeArchivedEvent`

8. `idempotent content type archive does not dispatch event`
    - create and archive content type
    - call archive again
    - assert zero new events

9. `rollback after content type archive does not dispatch event`
    - create DRAFT content type
    - inside new transaction call archive, then throw `RuntimeException`
    - assert zero `ContentTypeArchivedEvent`

### invalid transition

10. `invalid content type transition does not dispatch event`
    - create content type
    - archive it
    - attempt to activate archived content type
    - assert `InvalidContentTypeStatusTransitionException`
    - assert zero new events

### outside transaction

11. `content type outside transaction dispatches immediately`
    - call create outside any transaction
    - assert `ContentTypeCreatedEvent` is dispatched immediately

## 11. Documentation

Add or update a short task/ADR note explaining:

- content type events follow the same decorator pattern as site events
- content type events are dispatched through `CodexEventDispatcher`
- `DeferredEventDispatcher` controls transaction-aware delivery
- idempotent content type operations do not emit events
- invalid content type operations do not emit events
- content type event subscribers, audit, cache invalidation, workflow continuation, and external brokers are future work

## Constraints

- Follow CLAUDE.md conventions.
- Follow the same style used by existing `Site` event publishing.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No Custos implementation.
- No content item implementation.
- No content type field events.
- No content type version events.
- No TreeableResource implementation.
- No CodexResourcePath implementation.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable classes.
- Run the smallest relevant Maven test or compile command after implementation.