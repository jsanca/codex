# Task15: ContentRevision Foundation

## Objective

Introduce `ContentRevision` as the immutable snapshot of a content item's values.

Codex should separate the stable identity of content from the historical/value snapshots of that content.

`ContentItem` is the stable logical identity of the content.

`ContentRevision` is the immutable snapshot of the content values at a point in time.

This task refactors the initial `ContentItem` foundation so values move out of `ContentItem` and into `ContentRevision`.

This keeps the model symmetric with the existing ContentType model:

`ContentType`
`  -> stable schema identity / lifecycle / ownership`

`ContentTypeVersion`
`  -> immutable published schema snapshot`

`ContentItem`
`  -> stable content identity / lifecycle / ownership`

`ContentRevision`
`  -> immutable content values snapshot`

## Architectural direction

Do not store content values directly on `ContentItem`.

Store content values in `ContentRevision`.

`ContentItem` should point to revisions.

For this task:

- creating a content item creates the first working revision
- `ContentItem.currentWorkingRevisionId` points to that first revision
- `ContentItem.currentPublishedRevisionId` is null
- `ContentItem.status` remains `DRAFT`
- publishing is not implemented yet

Future work will implement publishing by moving or setting revision pointers.

## Scope

Implement:

- `ContentRevisionId`
- `ContentRevisionStatus`
- `ContentRevision`
- `ContentRevisionRepository`
- `MemoryContentRevisionRepository`
- update `ContentItem`
- update `CreateContentItemCommand` if needed
- update `CodexContentItemService`
- update `CodexRuntime` wiring if needed
- update tests

Do not implement:

- publish/unpublish
- workflow
- rollback
- revision diffing
- revision comparison
- revision comments
- revision locking
- localization
- variants
- relationships
- assets
- rendering
- search indexing
- revision events
- content item update events
- audit
- cache invalidation
- permission service
- Custos
- Spring integration
- persistence
- TreeableResource
- CodexResourcePath

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.identity.ContentRevisionId`
- `codex.codex.api.model.value.ContentRevisionStatus`
- `codex.codex.api.model.entity.ContentRevision`
- `codex.codex.api.model.entity.ContentItem`
- `codex.codex.api.model.command.CreateContentItemCommand`
- `codex.codex.internal.repository.ContentRevisionRepository`
- `codex.codex.internal.repository.MemoryContentRevisionRepository`
- `codex.codex.internal.service.CodexContentItemService`
- existing content item tests
- existing runtime tests

## 1. Create ContentRevisionId

Create:

`codex.codex.api.model.identity.ContentRevisionId`

Requirements:

- use a Java record
- wrap a `String value`
- validate non-null
- trim value
- validate non-blank
- provide static factory method:

`of(String value)`

- provide deterministic factory if consistent with existing identity style

Suggested deterministic id format:

`content-revision:{siteKey}:{contentTypeKey}:{contentItemKey}:r{revisionNumber}`

Example:

`content-revision:acme:blog-post:welcome-to-codex:r1`

If adding a typed deterministic factory, prefer value objects:

`forRevision(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey itemKey, int revisionNumber)`

Requirements for deterministic factory:

- validate siteKey non-null
- validate contentTypeKey non-null
- validate itemKey non-null
- validate revisionNumber >= 1
- do not use raw strings outside the factory

Do not use random UUIDs if the project is already using deterministic identities for similar resources.

## 2. Create ContentRevisionStatus

Create:

`codex.codex.api.model.value.ContentRevisionStatus`

Initial values:

- `WORKING`
- `PUBLISHED`
- `ARCHIVED`

Semantics:

`WORKING`
- revision exists as the current draft/working version
- not published for normal consumption

`PUBLISHED`
- revision is published and can be used for normal rendering/consumption

`ARCHIVED`
- revision is retained for history but retired from normal use

For this task, newly created revisions should be `WORKING`.

Do not implement publish/unpublish transitions in this task.

## 3. Create ContentRevision

Create:

`codex.codex.api.model.entity.ContentRevision`

The record shape should be equivalent to:

`ContentRevisionId id`
`ContentItemId contentItemId`
`SiteKey siteKey`
`ContentTypeKey contentTypeKey`
`ContentTypeVersionId contentTypeVersionId`
`ContentItemKey contentItemKey`
`int revisionNumber`
`ContentRevisionStatus status`
`Map<FieldKey, Object> values`
`ActorId createdBy`
`Instant createdAt`

Requirements:

- `id` must not be null
- `contentItemId` must not be null
- `siteKey` must not be null
- `contentTypeKey` must not be null
- `contentTypeVersionId` must not be null
- `contentItemKey` must not be null
- `revisionNumber` must be >= 1
- `status` defaults to `ContentRevisionStatus.WORKING` if null
- `values` defaults to `Map.of()` if null
- `values` must be defensively copied with `Map.copyOf(...)`
- values map must not allow null keys
- values map must not allow null values for this task
- `createdBy` must not be null
- `createdAt` defaults to `Instant.now()` if null
- builder must support all fields
- `copyOf(...)` should exist and preserve all fields
- `toString()` should include:
    - id
    - contentItemId
    - siteKey
    - contentTypeKey
    - contentTypeVersionId
    - contentItemKey
    - revisionNumber
    - status
    - value keys or value count
    - createdBy
    - createdAt

Do not store owner or updatedBy on `ContentRevision` in this task.

A revision is immutable once created.

If a new edit is needed later, it should create another revision.

## 4. Update ContentItem

Update `ContentItem` so it no longer stores content values directly.

Remove from `ContentItem` if present:

`Map<FieldKey, Object> values`

Add fields:

`ContentRevisionId currentWorkingRevisionId`
`ContentRevisionId currentPublishedRevisionId`

Requirements:

- `currentWorkingRevisionId` may be null only before the item is fully created
- after `CodexContentItemService.create(...)`, it must be set
- `currentPublishedRevisionId` may be null for DRAFT items
- builder must support both revision pointers
- `copyOf(...)` must preserve both revision pointers
- `toString()` should include both revision pointers
- existing metadata must remain:
    - id
    - siteKey
    - contentTypeKey
    - contentTypeVersionId
    - key
    - status
    - owner
    - createdBy
    - updatedBy
    - createdAt
    - updatedAt

For this task:

- new content items are created as `DRAFT`
- new content items have `currentWorkingRevisionId` set
- new content items have `currentPublishedRevisionId = null`

Do not implement publish behavior.

Do not implement live/working revision switching beyond first creation.

## 5. Create ContentRevisionRepository

Create:

`codex.codex.internal.repository.ContentRevisionRepository`

Contract should be equivalent to:

`ContentRevision save(ContentRevision revision)`

`Optional<ContentRevision> findById(ContentRevisionId id)`

`Optional<ContentRevision> findByContentItemAndRevision(ContentItemId contentItemId, int revisionNumber)`

`Optional<ContentRevision> findLatestWorking(ContentItemId contentItemId)`

`Optional<ContentRevision> findLatestPublished(ContentItemId contentItemId)`

`List<ContentRevision> findByContentItem(ContentItemId contentItemId)`

Requirements:

- use value objects, not raw strings
- validate null arguments
- validate revisionNumber >= 1
- no Spring
- no persistence framework
- no generic query API

Do not implement delete.

Do not implement update.

Revisions are intended to be append-only snapshots.

## 6. Create MemoryContentRevisionRepository

Create:

`codex.codex.internal.repository.MemoryContentRevisionRepository`

Requirements:

- implements `ContentRevisionRepository`
- uses `MemoryStore<ContentRevisionId, ContentRevision>`
- key resolver should be `ContentRevision::id`
- `save(...)` returns saved revision
- `findById(...)` delegates to store
- `findByContentItemAndRevision(...)` filters by `contentItemId + revisionNumber`
- `findLatestWorking(...)` returns highest revision number with status `WORKING`
- `findLatestPublished(...)` returns highest revision number with status `PUBLISHED`
- `findByContentItem(...)` returns immutable snapshot filtered by content item
- `findByContentItem(...)` should sort by revision number for deterministic behavior
- validate null arguments
- no Spring
- no external infrastructure

Do not create a second storage mechanism.

Use the existing `MemoryStore` pattern.

## 7. Update CreateContentItemCommand

Keep `CreateContentItemCommand.values`.

The command still receives the initial values needed to create the first revision.

Requirements:

- keep:
    - `SiteKey siteKey`
    - `ContentTypeKey contentTypeKey`
    - `ContentItemKey key`
    - `Map<FieldKey, Object> values`
- values continue to default to `Map.of()` if null
- values continue to be defensively copied
- values continue to reject null keys and null values

Do not add `ContentRevisionId` to the command.

Do not add `ContentTypeVersionId` to the command.

The service resolves the latest published `ContentTypeVersionId`.

The service creates the first `ContentRevision`.

## 8. Update CodexContentItemService constructor

Update `CodexContentItemService` to receive:

- `ContentItemRepository`
- `ContentRevisionRepository`
- `ContentTypeRepository`
- `ContentTypeVersionRepository`
- `Clock`
- identity generators or deterministic factories if needed

Validate dependencies with `Objects.requireNonNull`.

No Spring annotations.

No transaction annotations.

## 9. Update create behavior

Update `CodexContentItemService.create(...)`.

Current behavior may create `ContentItem` with values directly.

New behavior:

1. require non-null command
2. require non-null actor
3. check duplicate content item key by `siteKey + contentTypeKey + itemKey`
4. if duplicate exists, throw `ContentItemAlreadyExistsException`
5. load `ContentType` by `siteKey + contentTypeKey`
6. if missing, throw `NotFoundException`
7. validate content type is `ACTIVE`
8. validate content type has `latestPublishedVersionId`
9. load `ContentTypeVersion` by latest published version id
10. if missing, throw `NotFoundException`
11. validate content type version is `PUBLISHED`
12. validate command values against `ContentTypeVersion.fields`
13. compute:
    - `ContentItemId`
    - first revision number `1`
    - `ContentRevisionId`
14. create `ContentItem` with:
    - generated/deterministic id
    - siteKey from command
    - contentTypeKey from command
    - contentTypeVersionId from latest published content type version
    - key from command
    - status `ContentItemStatus.DRAFT`
    - currentWorkingRevisionId = first revision id
    - currentPublishedRevisionId = null
    - owner = actor.id()
    - createdBy = actor.id()
    - updatedBy = actor.id()
    - createdAt = clock.instant()
    - updatedAt = clock.instant()
15. create `ContentRevision` with:
    - generated/deterministic revision id
    - contentItemId
    - siteKey
    - contentTypeKey
    - contentTypeVersionId
    - contentItemKey
    - revisionNumber = 1
    - status `ContentRevisionStatus.WORKING`
    - values from command
    - createdBy = actor.id()
    - createdAt = clock.instant()
16. save the `ContentRevision`
17. save the `ContentItem`
18. return the saved `ContentItem`

Important:

This is a multi-write operation.

It must eventually run inside a transaction boundary.

For now, keep the implementation straightforward and rely on future transactional service wrapping.

## 10. Validation rules stay the same

Keep the existing content item creation validation rules.

Validate against `ContentTypeVersion.fields`, not `ContentType.fields`.

Rules:

### Unknown fields

If values contain a `FieldKey` not present in the version fields map:

- reject
- throw `ContentItemFieldValidationException`

### Required fields

If a field is marked `required` and the values map does not contain that `FieldKey`:

- reject
- throw `ContentItemFieldValidationException`

### Null values

For this task, null values are rejected at command/entity level.

### Type validation

Do not implement a full type validation engine in this task.

At minimum, preserve existing validation for:

- unknown fields
- missing required fields

## 11. Repository behavior

`ContentItemRepository` should still manage item identity.

`ContentRevisionRepository` should manage value snapshots.

Do not make `ContentItemRepository` responsible for revisions.

Do not store revision values in `ContentItemRepository`.

Do not make `ContentRevisionRepository` responsible for item metadata.

Keep responsibilities separate.

## 12. Update EventPublishingContentItemService if needed

`ContentItemCreatedEvent` should continue to be published after successful item creation.

The event should include the created item's:

- id
- siteKey
- contentTypeKey
- contentTypeVersionId
- key
- actor
- occurredAt

Do not include values in the event.

Do not include revision values in the event.

Optional:

If `ContentItemCreatedEvent` already exists and can be safely extended, consider adding:

`ContentRevisionId currentWorkingRevisionId`

Only do this if tests and event semantics remain simple.

Otherwise, leave the event unchanged.

## 13. Runtime wiring

Update `CodexRuntime` if it already wires `ContentItemService`.

Add:

- `MemoryContentRevisionRepository`

Pass it into:

- `CodexContentItemService`

Do not create a separate event dispatcher.

Do not create a separate executor.

Do not expose internal repositories publicly unless already planned.

## 14. Tests

Add or update plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repositories and real services.

### ContentRevisionId tests

Add tests for:

- rejects null value
- rejects blank value
- trims value
- `of(...)` factory works
- deterministic factory uses siteKey + contentTypeKey + itemKey + revisionNumber if implemented
- deterministic factory rejects revisionNumber less than 1

### ContentRevisionStatus tests

Add compile-level or simple tests for:

- `WORKING`
- `PUBLISHED`
- `ARCHIVED`

### ContentRevision tests

Add tests for:

- rejects null id
- rejects null contentItemId
- rejects null siteKey
- rejects null contentTypeKey
- rejects null contentTypeVersionId
- rejects null contentItemKey
- rejects revisionNumber less than 1
- defaults status to `WORKING`
- defaults values to empty map
- defensively copies values
- values accessor is immutable
- rejects null value map keys
- rejects null value map values
- rejects null createdBy
- defaults createdAt when null
- builder supports all fields
- `copyOf(...)` preserves all fields
- `toString()` does not dump full values if value count style is used

### ContentItem tests

Update tests:

- `ContentItem` no longer has values
- rejects null currentWorkingRevisionId only if constructor enforces it
- allows null currentPublishedRevisionId for DRAFT item
- builder supports currentWorkingRevisionId
- builder supports currentPublishedRevisionId
- `copyOf(...)` preserves revision pointers
- `toString()` includes revision pointers

If constructor allows null currentWorkingRevisionId to support staged construction, add service-level tests proving created items always have it set.

### MemoryContentRevisionRepository tests

Add tests for:

- save returns saved revision
- findById returns saved revision
- findById returns empty when missing
- findByContentItemAndRevision returns saved revision
- findByContentItemAndRevision returns empty when missing
- findLatestWorking returns highest working revision
- findLatestPublished returns highest published revision
- findLatestWorking ignores published and archived revisions
- findLatestPublished ignores working and archived revisions
- findByContentItem returns all revisions for item
- findByContentItem sorts by revisionNumber
- required arguments reject null
- revisionNumber less than 1 rejects where applicable

### CodexContentItemService tests

Update or add tests:

- create should create draft content item
- create should create first working revision
- created item should point to first working revision
- created item should have null currentPublishedRevisionId
- created revision should contain command values
- created revision should reference content item id
- created revision should reference content type version id
- created revision should have revisionNumber 1
- created revision should have status WORKING
- created revision should set createdBy from actor id
- content item should no longer store values directly
- create should reject duplicate item key and not create a revision
- create should reject missing content type and not create a revision
- create should reject DRAFT content type and not create a revision
- create should reject ARCHIVED content type and not create a revision
- create should reject missing content type version and not create a revision
- create should reject non-PUBLISHED content type version and not create a revision
- create should reject unknown field values and not create a revision
- create should reject missing required field and not create a revision

### Event tests

Update content item event tests if needed.

Ensure:

- content item created event still fires after successful create
- rollback still discards event
- invalid create still does not dispatch event
- event does not include values

### Runtime test

If `CodexRuntime` exposes content item service:

- create content type
- add required field
- activate content type
- create content item
- assert item has currentWorkingRevisionId
- assert item has null currentPublishedRevisionId
- assert no wiring errors

Do not expose repositories publicly only for this test unless already planned.

## 15. Documentation

Add or update a short note explaining:

- `ContentItem` is the stable logical identity of content
- `ContentRevision` is the immutable snapshot of content values
- `ContentItem` no longer stores values directly
- `ContentItem.currentWorkingRevisionId` points to the current draft revision
- `ContentItem.currentPublishedRevisionId` is null until publishing exists
- creating a content item creates revision `1` as `WORKING`
- content values are validated against `ContentTypeVersion.fields`
- publish/unpublish workflow is future work
- rollback and revision history operations are future work

## Constraints

- Follow CLAUDE.md conventions.
- Follow the style of existing Site, ContentType, and ContentItem implementations.
- Keep ContentItem and ContentRevision responsibilities separate.
- Use `ContentItemKey`.
- Use `Map<FieldKey, Object>` for revision values.
- Validate values against `ContentTypeVersion.fields`.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No Custos implementation.
- No workflow implementation.
- No publish/unpublish implementation.
- No revision events.
- No update events.
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