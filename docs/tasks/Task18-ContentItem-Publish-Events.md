# Task18: ContentItem Publish Events

## Objective

Add event publishing support for content item publishing.

`ContentItemService.publish(...)` was introduced as the first pointer-based publishing operation.

This task adds the domain event emitted after a successful publish:

`ContentItemPublishedEvent`

The event should be dispatched through the existing `CodexEventDispatcher` pipeline so it automatically benefits from:

- `DeferredEventDispatcher`
- `TransactionContext`
- after-commit event delivery
- rollback event discard
- immediate dispatch outside transactions

This task should not implement indexing, cache invalidation, audit, workflow, search, OpenSearch, embeddings, or external broker integration.

## Architectural direction

Publishing is a domain lifecycle operation.

Indexing, cache invalidation, audit, workflow continuation, and external publication are projection/subscriber concerns.

This task only emits the domain fact:

`Content item was published`

Future subscribers may react to this event.

Do not index directly inside `CodexContentItemService`.

Do not index directly inside `EventPublishingContentItemService`.

Do not add cache behavior.

Do not add audit behavior.

Do not add search behavior.

## Scope

Implement:

- `ContentItemPublishedEvent`
- event publishing in `EventPublishingContentItemService.publish(...)`
- tests for the event record
- tests for the event publishing decorator
- integration tests with `TransactionContext`
- documentation note or ADR update if appropriate

Do not implement:

- indexing subscribers
- cache invalidation subscribers
- audit subscribers
- workflow subscribers
- OpenSearch integration
- myIR integration
- embedding/vector index integration
- external broker publishing
- outbox
- search service
- publish workflow
- approvals
- rollback
- unpublish
- edit after publish
- revision diffing
- TreeableResource
- CodexResourcePath

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.event.ContentItemPublishedEvent`
- `codex.codex.internal.service.EventPublishingContentItemService`
- existing content item event tests
- existing content item integration tests
- existing runtime/integration tests if appropriate

## 1. Create ContentItemPublishedEvent

Create:

`codex.codex.api.model.event.ContentItemPublishedEvent`

Use a Java record.

The event should implement `CodexEvent`.

Fields:

- `ContentItemId id`
- `SiteKey siteKey`
- `ContentTypeKey contentTypeKey`
- `ContentTypeVersionId contentTypeVersionId`
- `ContentItemKey key`
- `ContentRevisionId publishedRevisionId`
- `Actor actor`
- `Instant occurredAt`

Required imports will likely include:

- `codex.codex.api.model.identity.ContentItemId`
- `codex.codex.api.model.identity.ContentItemKey`
- `codex.codex.api.model.identity.ContentRevisionId`
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

Do not include content values in this event.

Reason:

- values may be large
- values may be sensitive
- subscribers can load the published revision by `publishedRevisionId` if they need values
- search/indexing subscribers should resolve the canonical revision when they run

Example shape:

`public record ContentItemPublishedEvent(ContentItemId id, SiteKey siteKey, ContentTypeKey contentTypeKey, ContentTypeVersionId contentTypeVersionId, ContentItemKey key, ContentRevisionId publishedRevisionId, Actor actor, Instant occurredAt) implements CodexEvent`

## 2. Update EventPublishingContentItemService

Update:

`codex.codex.internal.service.EventPublishingContentItemService`

Add override for:

`publish(PublishContentItemCommand command, Actor actor)`

Requirements:

- require non-null command
- require non-null actor
- call delegate `publish`
- dispatch `ContentItemPublishedEvent` only after delegate returns successfully
- return the published content item
- if delegate throws, do not dispatch an event

Event payload should use the returned item:

- `result.id()`
- `result.siteKey()`
- `result.contentTypeKey()`
- `result.contentTypeVersionId()`
- `result.key()`
- `result.currentPublishedRevisionId()`
- `actor`
- `clock.instant()`

If `result.currentPublishedRevisionId()` is null after publish, treat that as an invalid delegate result and fail fast with an appropriate exception such as `IllegalStateException`.

Do not include values.

Do not load the revision in this decorator.

The decorator only publishes the fact using data from the returned item.

## 3. Idempotency behavior

Publishing an already published item may be idempotent in `CodexContentItemService`.

Idempotent publish should not emit another `ContentItemPublishedEvent`.

The event publishing decorator must avoid emitting an event when the publish operation made no state change.

Use before/after state comparison.

Expected approach:

1. read previous item before delegate call using:

`delegate.findByKey(command.siteKey(), command.contentTypeKey(), command.key(), actor)`

2. call delegate `publish`
3. compare previous and result

Publish event should be emitted only when:

- previous item exists
- previous item was not already published with the same `currentPublishedRevisionId`
- result status is `ContentItemStatus.PUBLISHED`
- result current published revision id is not null

Do not duplicate full business rules in the decorator.

The decorator only observes before/after state to decide whether the event should be published.

If previous item is missing, let delegate throw the authoritative `NotFoundException`.

## 4. Suggested helper method

To keep `EventPublishingContentItemService` readable, add a small private helper if useful:

`shouldPublishEvent(Optional<ContentItem> previous, ContentItem result)`

Rules:

Return false when:

- previous is empty
- result is not `PUBLISHED`
- result current published revision id is null
- previous item was already `PUBLISHED`
- previous current published revision id equals result current published revision id

Return true when:

- previous exists
- result is `PUBLISHED`
- result current published revision id is not null
- previous was not already published with the same published revision id

Do not overbuild a validation framework.

Keep it simple.

## 5. ForwardingContentItemService

If `ForwardingContentItemService` already forwards `publish`, no changes are needed.

If it does not, add default forwarding for:

`publish(PublishContentItemCommand command, Actor actor)`

Do not add event behavior there.

## 6. Invalid operations

Invalid publish operations must not publish events.

Examples:

- missing content item
- archived content item
- item has no working revision
- missing working revision
- working revision is archived
- null command
- null actor

In all such cases, no `ContentItemPublishedEvent` should be dispatched.

## 7. Runtime wiring

If `CodexRuntime` already wires:

`EventPublishingContentItemService`

then no runtime wiring changes should be needed.

If `CodexRuntime` still exposes raw `CodexContentItemService`, update it so the exposed service is:

`EventPublishingContentItemService`

Expected wiring:

`MemoryContentItemRepository`
`MemoryContentRevisionRepository`
`CodexContentItemService`
`EventPublishingContentItemService`

Use the existing runtime:

- `DeferredEventDispatcher`
- shared event delegate / recorder
- shared executor
- shared clock

Do not create a second dispatcher.

Do not create a second executor.

Site, ContentType, and ContentItem should share the same event dispatcher pipeline.

## 8. Tests

Add or update plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repositories and real services when practical.

### ContentItemPublishedEvent tests

Add tests for:

- rejects null id
- rejects null siteKey
- rejects null contentTypeKey
- rejects null contentTypeVersionId
- rejects null key
- rejects null publishedRevisionId
- rejects null actor
- rejects null occurredAt

### EventPublishingContentItemService tests

Add tests for:

- publish delegates and publishes `ContentItemPublishedEvent` when publish changes state
- publish does not publish when delegate throws
- idempotent publish does not publish event
- required method arguments reject null
- constructor arguments reject null if not already covered

Verify event payload:

- id
- siteKey
- contentTypeKey
- contentTypeVersionId
- key
- publishedRevisionId
- actor
- occurredAt

### Integration tests with TransactionContext

Add or update content item event integration tests.

Use real components:

- `TransactionContext`
- `EventPublishingContentItemService`
- `CodexContentItemService`
- `MemoryContentItemRepository`
- `MemoryContentRevisionRepository`
- `MemoryContentTypeRepository`
- `MemoryContentTypeVersionRepository`
- `DeferredEventDispatcher`
- recording dispatcher
- synchronous test executor
- fixed clock

Or use `CodexRuntime.inMemory()` if it already exposes required services and recorded events.

#### Test: publish inside transaction does not dispatch before commit

Flow:

1. create site
2. create content type
3. add required field
4. activate content type
5. create content item
6. clear recorded events
7. inside transaction, call publish
8. before transaction returns, assert no publish event is recorded
9. after commit, assert one `ContentItemPublishedEvent`

#### Test: publish rollback does not dispatch event

Flow:

1. create site
2. create content type
3. add required field
4. activate content type
5. create content item
6. clear recorded events
7. inside transaction, call publish
8. throw `RuntimeException("forced rollback")`
9. after catch, assert no `ContentItemPublishedEvent`

Note:

This validates event rollback only.

Do not assert repository rollback unless repository transactions exist.

#### Test: publish outside transaction dispatches immediately

Flow:

1. create site
2. create content type
3. add required field
4. activate content type
5. create content item
6. clear recorded events
7. call publish outside transaction
8. assert `ContentItemPublishedEvent` is recorded immediately

#### Test: idempotent publish does not dispatch event

Flow:

1. create site
2. create content type
3. add required field
4. activate content type
5. create content item
6. publish it once
7. clear recorded events
8. publish it again
9. assert no `ContentItemPublishedEvent`

#### Test: invalid publish does not dispatch event

At minimum, test one invalid case:

- publish missing item
- or publish archived item
- or publish item with missing working revision

Assert:

- expected exception is thrown
- no `ContentItemPublishedEvent` is recorded

## 9. Event assertions

Do not assert event ordering unless the dispatcher explicitly guarantees order.

Prefer helper methods:

- `eventsOfType(ContentItemPublishedEvent.class)`
- `assertNoEventsOfType(ContentItemPublishedEvent.class)`
- `assertSingleEventOfType(ContentItemPublishedEvent.class)`

Do not require exact total event count unless the test clears recorded events immediately before the operation being tested.

## 10. Documentation / ADR

Add or update a short note explaining:

- `ContentItemPublishedEvent` represents the domain fact that a content item was published
- event does not include values
- subscribers can resolve the published revision by `publishedRevisionId`
- this event is the future trigger for:
    - indexing
    - cache invalidation
    - audit
    - workflow continuation
    - external publication
- indexing/search/cache are not implemented in this task

If updating ADR-007, mention that `ContentItemPublishedEvent` is the primary event expected to feed public content indexing.

If creating a new ADR, suggested title:

`ADR-008: ContentItem Publish Event`

Status may be `Draft` until code and tests are accepted.

## Constraints

- Follow CLAUDE.md conventions.
- Follow existing event publishing style for Site, ContentType, and ContentItemCreatedEvent.
- Do not include content values in events.
- Do not implement indexing.
- Do not implement cache invalidation.
- Do not implement audit.
- Do not implement workflow.
- Do not implement search.
- Do not implement OpenSearch.
- Do not implement embeddings.
- Do not implement external broker integration.
- Do not implement outbox.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No Custos implementation.
- No TreeableResource implementation.
- No CodexResourcePath implementation.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable classes.
- Run the smallest relevant Maven test command after implementation.