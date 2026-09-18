# Task13: ContentItem Foundation

## Objective

Implement the first foundation for `ContentItem`.

A `ContentItem` represents an instance of content created from a published `ContentTypeVersion`.

This task introduces the initial content item model, command, repository, service, and validation against a published content type schema snapshot.

The goal is to create content items safely against immutable `ContentTypeVersion.fields`, not against the mutable `ContentType.fields`.

## Architectural direction

Keep the identity vocabulary consistent across Codex:

- `SiteKey`
- `ContentTypeKey`
- `FieldKey`
- `ContentItemKey`

Use `ContentItemKey`, not `Slug`, `Path`, `Identifier`, or other names.

Avoid introducing different vocabulary unless the domain truly requires it.

A `ContentItem` belongs to:

`siteKey + contentTypeKey + contentItemKey`

A `ContentItem` must reference a stable published schema:

`contentTypeVersionId`

This prepares Codex for future content revisions, workflow, publishing, localization, treeable resources, and rendering.

## Scope

Implement:

- `ContentItemId`
- `ContentItemKey`
- `ContentItemStatus`
- `ContentItem`
- `CreateContentItemCommand`
- `ContentItemRepository`
- `MemoryContentItemRepository`
- `ContentItemService`
- `CodexContentItemService`
- basic validation against `ContentTypeVersion.fields`
- tests

Do not implement:

- ContentRevision
- workflow
- publish/unpublish lifecycle
- localization behavior
- relationships
- assets
- rendering
- search indexing
- event publishing
- audit
- cache invalidation
- permission service
- Custos
- Spring integration
- persistence
- TreeableResource
- CodexResourcePath
- JavaScript extensions

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.identity.ContentItemId`
- `codex.codex.api.model.identity.ContentItemKey`
- `codex.codex.api.model.value.ContentItemStatus`
- `codex.codex.api.model.entity.ContentItem`
- `codex.codex.api.model.command.CreateContentItemCommand`
- `codex.codex.api.model.service.ContentItemService`
- `codex.codex.internal.repository.ContentItemRepository`
- `codex.codex.internal.repository.MemoryContentItemRepository`
- `codex.codex.internal.service.CodexContentItemService`
- `codex.codex.internal.service.ContentItemValidationException`
- `codex.codex.internal.service.ContentItemFieldValidationException`

## 1. Create ContentItemId

Create:

`codex.codex.api.model.identity.ContentItemId`

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

`content-item:{siteKey}:{contentTypeKey}:{contentItemKey}`

Example:

`content-item:acme:blog-post:welcome-to-codex`

If adding a typed deterministic factory, prefer value objects:

`forItem(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey itemKey)`

Do not use random UUIDs if the project is already using deterministic identities for similar resources.

## 2. Create ContentItemKey

Create:

`codex.codex.api.model.identity.ContentItemKey`

Requirements:

- use a Java record
- wrap a `String value`
- validate non-null
- trim and normalize to lowercase
- validate non-blank
- use a safe key format similar to `SiteKey` and `ContentTypeKey`
- provide static factory method:

`of(String value)`

Examples:

- `welcome-to-codex`
- `home-page`
- `about-us`
- `product-123`

This key is not a full path.

Future `TreeableResource` or `CodexResourcePath` support may use this key as one segment in a larger path.

## 3. Create ContentItemStatus

Create:

`codex.codex.api.model.value.ContentItemStatus`

Initial values:

- `DRAFT`
- `PUBLISHED`
- `ARCHIVED`

Semantics:

- `DRAFT`: item exists but is not published
- `PUBLISHED`: item is published and available for normal consumption
- `ARCHIVED`: item is retired from normal use

For this task, only create items as `DRAFT`.

Do not implement publish/unpublish/archive operations yet.

## 4. Create ContentItem

Create:

`codex.codex.api.model.entity.ContentItem`

The record shape should be equivalent to:

`ContentItemId id`
`SiteKey siteKey`
`ContentTypeKey contentTypeKey`
`ContentTypeVersionId contentTypeVersionId`
`ContentItemKey key`
`ContentItemStatus status`
`Map<FieldKey, Object> values`
`ActorId owner`
`ActorId createdBy`
`ActorId updatedBy`
`Instant createdAt`
`Instant updatedAt`

Requirements:

- `id` must not be null
- `siteKey` must not be null
- `contentTypeKey` must not be null
- `contentTypeVersionId` must not be null
- `key` must not be null
- `status` defaults to `ContentItemStatus.DRAFT` if null
- `values` defaults to `Map.of()` if null
- `values` must be defensively copied with `Map.copyOf(...)`
- values map must not allow null keys
- values map must not allow null values for this task
- `owner` must not be null
- `createdBy` must not be null
- `updatedBy` must not be null
- `createdAt` defaults to `Instant.now()` if null
- `updatedAt` defaults to `createdAt` if null
- builder must support all fields
- `copyOf(...)` should exist and preserve all fields
- `toString()` should include:
    - id
    - siteKey
    - contentTypeKey
    - contentTypeVersionId
    - key
    - status
    - value keys or value count
    - owner
    - createdBy
    - updatedBy
    - createdAt
    - updatedAt

Do not implement localized values in this task.

Do not implement nested content values in this task.

Do not implement relationships in this task.

## 5. Create CreateContentItemCommand

Create:

`codex.codex.api.model.command.CreateContentItemCommand`

Fields:

- `SiteKey siteKey`
- `ContentTypeKey contentTypeKey`
- `ContentItemKey key`
- `Map<FieldKey, Object> values`

Requirements:

- validate `siteKey` non-null
- validate `contentTypeKey` non-null
- validate `key` non-null
- `values` defaults to `Map.of()` if null
- defensively copy `values`
- reject null field keys
- reject null field values for this task
- provide typed factory:

`of(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key, Map<FieldKey, Object> values)`

- provide string convenience factory if consistent with project style:

`of(String siteKey, String contentTypeKey, String itemKey, Map<FieldKey, Object> values)`

Do not include `Actor` in the command.

The actor is passed separately to the service.

Do not include `ContentTypeVersionId` in the command.

The service should resolve the latest published content type version.

## 6. Create ContentItemRepository

Create:

`codex.codex.internal.repository.ContentItemRepository`

Contract should be equivalent to:

`ContentItem save(ContentItem item)`

`Optional<ContentItem> findByKey(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key)`

`boolean existsByKey(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key)`

`List<ContentItem> findByContentType(SiteKey siteKey, ContentTypeKey contentTypeKey)`

`List<ContentItem> findAll()`

Requirements:

- use value objects, not raw strings
- validate null arguments
- no Spring
- no persistence framework
- no generic query API

Logical identity:

`siteKey + contentTypeKey + contentItemKey`

## 7. Create MemoryContentItemRepository

Create:

`codex.codex.internal.repository.MemoryContentItemRepository`

Requirements:

- implements `ContentItemRepository`
- stores items in memory
- use a private key record if useful:

`private record RepositoryKey(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey itemKey)`

- `save(...)` stores by `item.siteKey() + item.contentTypeKey() + item.key()`
- `save(...)` returns saved item
- `findByKey(...)` returns by combined key
- `existsByKey(...)` returns by combined key
- `findByContentType(...)` returns immutable snapshot filtered by site and content type
- `findAll()` returns immutable snapshot
- validate null arguments
- no Spring
- no external infrastructure

Ordering:

- `findByContentType(...)` may return insertion-independent order
- if easy, sort by `ContentItemKey.value()` for deterministic tests

## 8. Create ContentItemService

Create:

`codex.codex.api.model.service.ContentItemService`

Contract:

`ContentItem create(CreateContentItemCommand command, Actor actor)`

`Optional<ContentItem> findByKey(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key, Actor actor)`

`List<ContentItem> findByContentType(SiteKey siteKey, ContentTypeKey contentTypeKey, Actor actor)`

`List<ContentItem> findAll(Actor actor)`

Requirements:

- service is actor-aware
- use `codex.fundamentum.api.model.Actor`
- do not use Spring Security, Principal, JWT, Auth0, or framework concepts
- do not add permission service yet
- do not add event publishing yet

## 9. Create CodexContentItemService

Create:

`codex.codex.internal.service.CodexContentItemService`

Requirements:

- `final`
- implements `ContentItemService`
- constructor receives:
    - `ContentItemRepository`
    - `ContentTypeRepository`
    - `ContentTypeVersionRepository`
    - `Clock`
    - identity generator or factory if needed
- validate constructor dependencies with `Objects.requireNonNull`
- use SLF4J logging
- log public mutating operations at debug level
- validate all method inputs with `Objects.requireNonNull`
- no Spring annotations
- no transaction annotations

## 10. Create behavior

Implement `create(CreateContentItemCommand command, Actor actor)`.

Behavior:

1. require non-null command
2. require non-null actor
3. check duplicate content item key by `siteKey + contentTypeKey + itemKey`
4. if duplicate exists, throw semantic exception
5. load `ContentType` by `siteKey + contentTypeKey`
6. if missing, throw `NotFoundException`
7. validate content type is `ACTIVE`
8. validate content type has `latestPublishedVersionId`
9. load `ContentTypeVersion` by latest published version id
10. if missing, throw `NotFoundException`
11. validate command values against `ContentTypeVersion.fields`
12. create `ContentItem` with:
    - generated/deterministic id
    - siteKey from command
    - contentTypeKey from command
    - contentTypeVersionId from latest published content type
    - key from command
    - status `ContentItemStatus.DRAFT`
    - values from command
    - owner = actor.id()
    - createdBy = actor.id()
    - updatedBy = actor.id()
    - createdAt = clock.instant()
    - updatedAt = clock.instant()
13. save and return item

## 11. Content type validation

When creating a content item:

- content type must exist
- content type must be `ContentTypeStatus.ACTIVE`
- content type must have a latest published version id
- referenced content type version must exist
- referenced content type version must be `ContentTypeVersionStatus.PUBLISHED`

If content type is:

- `DRAFT`: reject
- `ARCHIVED`: reject

Suggested exception:

`InvalidContentItemCreationException`

or:

`InvalidContentTypeForContentItemException`

Keep it simple and semantic.

Use `NotFoundException` for missing content type or missing version.

## 12. Field value validation

Validate `command.values()` against `ContentTypeVersion.fields()`.

Rules for this task:

### Unknown fields

If values contain a `FieldKey` not present in the version fields map:

- reject
- throw `ContentItemFieldValidationException`

Example:

`Unknown field summary for content type blog-post`

### Required fields

If a field is marked `required` and the values map does not contain that `FieldKey`:

- reject
- throw `ContentItemFieldValidationException`

### Null values

For this task, null values are rejected at command/entity level.

Do not implement optional null semantics yet.

### Type validation

Keep type validation minimal.

If `FieldType` already exposes enough information to validate basic Java types, implement only simple obvious checks.

If `FieldType` is not ready for validation, do not invent a large validation system.

At minimum, this task must validate:

- unknown fields
- missing required fields

Do not implement:

- regex validation
- min/max validation
- length validation
- localized validation
- repeatable value validation
- relationship validation
- asset validation
- nested object validation

Those will come later through a dedicated validation engine.

## 13. Duplicate item exception

Create semantic exception if it does not already exist:

`ContentItemAlreadyExistsException`

Location:

`codex.codex.internal.service`

Requirements:

- runtime exception
- simple and semantic
- useful message

Example:

`Content item welcome-to-codex already exists for content type blog-post in site acme`

## 14. Content item validation exceptions

Create semantic exceptions if they do not already exist:

`InvalidContentItemCreationException`

`ContentItemFieldValidationException`

Location:

`codex.codex.internal.service`

Requirements:

- runtime exceptions
- simple and semantic
- useful messages
- do not create a large hierarchy unless one already exists

## 15. Validation helper methods

Inside `CodexContentItemService`, keep validation readable.

Suggested private helpers:

`findContentTypeRequired(...)`

`findVersionRequired(...)`

`validateContentTypeCanCreateItems(...)`

`validateVersionCanCreateItems(...)`

`validateValues(...)`

`validateNoUnknownFields(...)`

`validateRequiredFields(...)`

Do not introduce a full validation pipeline in this task.

Do not introduce reusable validation framework yet.

## 16. Idempotency

Do not make `create` idempotent.

If an item with the same logical identity already exists, throw `ContentItemAlreadyExistsException`.

## 17. Events

Do not add content item events in this task.

Do not create:

- `ContentItemCreatedEvent`
- `EventPublishingContentItemService`
- `ForwardingContentItemService`

Those can be added later once the foundation is stable.

## 18. Runtime

If `CodexRuntime` already wires content type repositories and services, update it only if straightforward.

Possible wiring:

- `MemoryContentItemRepository`
- `CodexContentItemService`
- expose `ContentItemService contentItemService()`

Do not add event publishing for content items.

Do not create additional dispatchers.

Do not create additional executors.

Do not expose internal repositories publicly unless already planned.

## 19. Tests

Add or update plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repositories and real services.

### ContentItemKey tests

Add tests for:

- rejects null value
- rejects blank value
- trims value
- normalizes to lowercase
- accepts safe key
- rejects unsafe key if key validation exists
- `of(...)` factory works

### ContentItemId tests

Add tests for:

- rejects null value
- rejects blank value
- `of(...)` factory works
- deterministic factory uses siteKey + contentTypeKey + itemKey if implemented

### ContentItem tests

Add tests for:

- rejects null id
- rejects null siteKey
- rejects null contentTypeKey
- rejects null contentTypeVersionId
- rejects null key
- defaults status to `DRAFT`
- defaults values to empty map
- defensively copies values
- values accessor is immutable
- rejects null value map keys
- rejects null value map values
- rejects null owner
- rejects null createdBy
- rejects null updatedBy
- defaults createdAt when null
- defaults updatedAt to createdAt when null
- builder supports all fields
- `copyOf(...)` preserves all fields

### CreateContentItemCommand tests

Add tests for:

- requires non-null siteKey
- requires non-null contentTypeKey
- requires non-null key
- defaults values to empty map
- defensively copies values
- values accessor is immutable
- rejects null value map keys
- rejects null value map values
- typed factory works
- string factory works if implemented

### MemoryContentItemRepository tests

Add tests for:

- save returns saved item
- findByKey returns saved item
- findByKey returns empty when missing
- findByKey returns empty when siteKey differs
- findByKey returns empty when contentTypeKey differs
- existsByKey returns true when saved
- existsByKey returns false when missing
- same content item key can exist under different content types
- same content item key can exist under different sites
- findByContentType returns only items for that site and content type
- findAll returns all saved items
- required arguments reject null

### CodexContentItemService tests

Set up tests using real in-memory repositories.

Test fixtures should create:

- an ACTIVE content type
- at least one published `ContentTypeVersion`
- fields map on the version

Add tests for:

- create should create draft content item
- create should set siteKey
- create should set contentTypeKey
- create should set contentTypeVersionId from latest published version
- create should set owner from actor id
- create should set createdBy from actor id
- create should set updatedBy from actor id
- create should preserve provided field values
- create should reject duplicate item key in same site/content type
- create should allow same item key under different content type
- create should allow same item key under different site
- create should reject missing content type
- create should reject DRAFT content type
- create should reject ARCHIVED content type
- create should reject content type without latest published version id
- create should reject missing content type version
- create should reject non-PUBLISHED content type version
- create should reject unknown field values
- create should reject missing required field
- create should allow missing optional field
- findByKey should return existing item
- findByKey should return empty when missing
- findByContentType should return only matching items
- findAll should return all items
- required arguments reject null

### Runtime test

If `CodexRuntime` exposes `ContentItemService`, add a small integration test:

- create content type
- add required field
- activate content type
- create content item with required field value
- assert item is created and references latest published version

Do not assert content item events in this task.

## 20. Documentation

Add or update a short note explaining:

- `ContentItemKey` follows the same key vocabulary as `SiteKey`, `ContentTypeKey`, and `FieldKey`
- content item logical identity is `siteKey + contentTypeKey + contentItemKey`
- content items reference `ContentTypeVersionId`
- content items validate against `ContentTypeVersion.fields`, not mutable `ContentType.fields`
- first validation rules are unknown-field and missing-required-field checks
- deeper field validation is future work
- content revisions are future work
- workflow is future work
- content item events are future work

## Constraints

- Follow CLAUDE.md conventions.
- Follow the style of the existing Site and ContentType implementations.
- Use `ContentItemKey`.
- Use `Map<FieldKey, Object>` for values.
- Validate against `ContentTypeVersion.fields`.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No Custos implementation.
- No ContentRevision implementation.
- No workflow implementation.
- No content item events.
- No event publishing wrapper for content items.
- No audit.
- No cache invalidation.
- No TreeableResource implementation.
- No CodexResourcePath implementation.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable classes.
- Run the smallest relevant Maven test or compile command after implementation.