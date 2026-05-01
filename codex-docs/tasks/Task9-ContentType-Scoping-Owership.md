# Task9: ContentType Scoping and Ownership

## Objective

Refine the `ContentType` foundation so content types are no longer anonymous/global by accident.

A `ContentType` must belong to a `SiteKey` scope and must record basic ownership metadata.

Global content types are represented by using `SiteKey.SYSTEM`.

This task should not introduce fields, versioning, composition, content items, events, tree paths, or persistence.

## Architectural direction

A content type is scoped by site.

The logical identity of a content type is:

`siteKey + contentTypeKey`

This allows:

- global content types through `SiteKey.SYSTEM`
- site-specific content types through a regular `SiteKey`
- future treeable/resource-path support
- future content type versioning
- future content type composition
- future permission and ownership rules

Do not introduce `Scope.GLOBAL` or `Scope.SITE`.

Use `SiteKey.SYSTEM` for global content types.

## Scope

Implement:

- `SiteKey siteKey` in `ContentType`
- `ActorId owner` in `ContentType`
- `ActorId createdBy` in `ContentType`
- `ActorId updatedBy` in `ContentType`
- `SiteKey siteKey` in `CreateContentTypeCommand`
- repository lookup by `siteKey + contentTypeKey`
- service creation metadata from `Actor`
- tests

Do not implement:

- fields/schema definitions
- versioning
- composition
- content items
- content type events
- event publishing wrapper for content types
- TreeableResource
- CodexResourcePath
- resource protocol/scheme
- permission service
- Spring integration
- persistence

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.entity.ContentType`
- `codex.codex.api.model.command.CreateContentTypeCommand`
- `codex.codex.internal.repository.ContentTypeRepository`
- `codex.codex.internal.repository.MemoryContentTypeRepository`
- `codex.codex.internal.service.CodexContentTypeService`
- existing tests for content types

## 1. Update ContentType

Update `ContentType` to include site scope and ownership metadata.

Add fields:

- `SiteKey siteKey`
- `ActorId owner`
- `ActorId createdBy`
- `ActorId updatedBy`

The final record shape should be equivalent to:

`ContentTypeId id`
`SiteKey siteKey`
`ContentTypeKey key`
`String displayName`
`ContentTypeStatus status`
`ActorId owner`
`ActorId createdBy`
`ActorId updatedBy`
`Instant createdAt`
`Instant updatedAt`

Requirements:

- `id` must not be null
- `siteKey` must not be null
- `key` must not be null
- `displayName` must not be null or blank
- `status` defaults to `ContentTypeStatus.DRAFT` if null
- `owner` must not be null
- `createdBy` must not be null
- `updatedBy` must not be null
- `createdAt` defaults to `Instant.now()` if null
- `updatedAt` defaults to `createdAt` if null
- `displayName` must be trimmed
- builder must support all new fields
- `copyOf(...)` must preserve all new fields
- `toString()` should include `siteKey`, `owner`, `createdBy`, and `updatedBy`

Do not add fields/schema definitions in this task.

Do not add version fields in this task.

## 2. Update CreateContentTypeCommand

Update `CreateContentTypeCommand` to include `SiteKey`.

The command should contain:

- `SiteKey siteKey`
- `ContentTypeKey key`
- `String displayName`

Requirements:

- validate `siteKey` with `Objects.requireNonNull`
- validate `key` with `Objects.requireNonNull`
- validate `displayName` with non-null, trim, and non-blank validation
- provide factory method with typed keys:

`CreateContentTypeCommand.of(SiteKey siteKey, ContentTypeKey key, String displayName)`

- provide convenience factory method from strings if consistent with existing command style:

`CreateContentTypeCommand.of(String siteKey, String key, String displayName)`

The actor should not be part of the command.

The actor is passed to the service method separately.

## 3. Global ContentTypes

Global content types are represented by:

`siteKey = SiteKey.SYSTEM`

Do not create `ContentTypeScope`.

Do not create `Scope.GLOBAL`.

Do not use raw string `"system"` outside `SiteKey`.

Add JavaDoc to `CreateContentTypeCommand` or `ContentType` explaining:

- `SiteKey.SYSTEM` represents the global/system scope
- regular `SiteKey` values represent site-specific content types

## 4. Update ContentTypeRepository

Update `ContentTypeRepository` so lookups use both site scope and content type key.

The contract should be equivalent to:

`ContentType save(ContentType contentType)`

`Optional<ContentType> findByKey(SiteKey siteKey, ContentTypeKey key)`

`boolean existsByKey(SiteKey siteKey, ContentTypeKey key)`

`List<ContentType> findAll()`

Optional, only if useful and simple:

`List<ContentType> findBySiteKey(SiteKey siteKey)`

Requirements:

- repository methods must use `SiteKey` and `ContentTypeKey`
- do not use raw `String`
- validate null arguments
- do not add generic iterator/filter API
- do not add persistence framework concerns

## 5. Update MemoryContentTypeRepository

Update `MemoryContentTypeRepository` to use logical identity:

`siteKey + contentTypeKey`

Implementation guidance:

- use a private key record if useful, for example:

`private record RepositoryKey(SiteKey siteKey, ContentTypeKey contentTypeKey)`

- map should be keyed by the combined key
- `save(...)` stores by `contentType.siteKey()` and `contentType.key()`
- `save(...)` returns the saved content type
- `findByKey(...)` returns by combined key
- `existsByKey(...)` returns by combined key
- `findAll()` returns immutable snapshot
- if `findBySiteKey(...)` is added, return immutable snapshot filtered by `siteKey`

Do not change `ContentTypeKey` to include site information.

Keep `SiteKey` and `ContentTypeKey` separate.

## 6. Update CodexContentTypeService

Update `CodexContentTypeService` to use scoped lookup.

### create

Behavior:

- require non-null command
- require non-null actor
- check duplicate using `command.siteKey()` and `command.key()`
- if a content type with the same `siteKey + key` exists, throw `ContentTypeAlreadyExistsException`
- create new `ContentType` with:
    - generated id
    - `siteKey` from command
    - `key` from command
    - `displayName` from command
    - `status = ContentTypeStatus.DRAFT`
    - `owner = actor.id()`
    - `createdBy = actor.id()`
    - `updatedBy = actor.id()`
    - `createdAt = clock.instant()`
    - `updatedAt = clock.instant()`
- save and return it

### activate

Behavior:

- require non-null command
- require non-null actor
- load by `command.siteKey()` and `command.key()` if the activate command has siteKey
- if existing activate command does not yet have siteKey, update it in this task
- if missing, throw `NotFoundException`
- if already `ACTIVE`, return existing content type without saving
- if `ARCHIVED`, throw `InvalidContentTypeStatusTransitionException`
- if `DRAFT`, save a copy with:
    - `status = ContentTypeStatus.ACTIVE`
    - `updatedBy = actor.id()`
    - `updatedAt = clock.instant()`

### archive

Behavior:

- require non-null command
- require non-null actor
- load by `command.siteKey()` and `command.key()` if the archive command has siteKey
- if existing archive command does not yet have siteKey, update it in this task
- if missing, throw `NotFoundException`
- if already `ARCHIVED`, return existing content type without saving
- if `DRAFT` or `ACTIVE`, save a copy with:
    - `status = ContentTypeStatus.ARCHIVED`
    - `updatedBy = actor.id()`
    - `updatedAt = clock.instant()`

### findByKey

Update the service contract if necessary so `findByKey` receives both:

- `SiteKey siteKey`
- `ContentTypeKey key`
- `Actor actor`

Behavior:

- require non-null `siteKey`
- require non-null `key`
- require non-null `actor`
- delegate to repository

### findAll

Behavior:

- require non-null `actor`
- delegate to repository

Optional:

If `findBySiteKey` is added to the repository and service, require non-null `siteKey` and `actor`.

## 7. Update ContentTypeService

Update `ContentTypeService` to reflect scoped content type identity.

Expected contract:

`ContentType create(CreateContentTypeCommand command, Actor actor)`

`ContentType activate(ActivateContentTypeCommand command, Actor actor)`

`ContentType archive(ArchiveContentTypeCommand command, Actor actor)`

`Optional<ContentType> findByKey(SiteKey siteKey, ContentTypeKey key, Actor actor)`

`List<ContentType> findAll(Actor actor)`

Optional:

`List<ContentType> findBySiteKey(SiteKey siteKey, Actor actor)`

If `ActivateContentTypeCommand` and `ArchiveContentTypeCommand` currently only contain `ContentTypeKey`, update them to include `SiteKey`.

## 8. Update ActivateContentTypeCommand and ArchiveContentTypeCommand

Update these commands to include `SiteKey`.

Each command should contain:

- `SiteKey siteKey`
- `ContentTypeKey key`

Requirements:

- validate `siteKey` non-null
- validate `key` non-null
- provide typed factory:

`of(SiteKey siteKey, ContentTypeKey key)`

- provide string convenience factory if consistent with project style:

`of(String siteKey, String key)`

Do not include `Actor` in these commands.

## 9. Identity Generator

If there is a `ContentTypeIdentityGenerator`, update it so identity generation considers site scope.

A deterministic id should be based on:

`siteKey + contentTypeKey`

not just:

`contentTypeKey`

If there is no identity generator yet and the current implementation generates ids elsewhere, keep the existing style but make sure two content types with the same key in different sites can have different ids.

Examples:

- `system + blog-post`
- `acme + blog-post`

should not collide.

## 10. Tests

Update existing content type tests and add new ones.

Use plain JUnit 5.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repositories.

### ContentType tests

Add or update tests for:

- constructor rejects null `siteKey`
- constructor rejects null `owner`
- constructor rejects null `createdBy`
- constructor rejects null `updatedBy`
- builder supports `siteKey`
- builder supports `owner`
- builder supports `createdBy`
- builder supports `updatedBy`
- `copyOf(...)` preserves `siteKey`
- `copyOf(...)` preserves `owner`
- `copyOf(...)` preserves `createdBy`
- `copyOf(...)` preserves `updatedBy`
- `SiteKey.SYSTEM` can be used as global content type scope

### CreateContentTypeCommand tests

Add or update tests for:

- requires non-null `siteKey`
- requires non-null `key`
- requires non-null/non-blank `displayName`
- trims `displayName`
- supports typed factory
- supports string factory if implemented
- supports `SiteKey.SYSTEM`

### Repository tests

Add or update tests for `MemoryContentTypeRepository`:

- save returns saved content type
- findByKey returns saved content type using `siteKey + key`
- findByKey returns empty when siteKey differs
- existsByKey returns true when saved with same `siteKey + key`
- existsByKey returns false when siteKey differs
- same `ContentTypeKey` can exist under different `SiteKey` values
- findAll returns all saved content types
- required arguments should not accept null

If `findBySiteKey(...)` is implemented:

- findBySiteKey returns only content types for that site
- findBySiteKey returns empty list when no content types exist for that site

### Service tests

Add or update tests for `CodexContentTypeService`:

- create should create draft content type with siteKey
- create should set owner from actor id
- create should set createdBy from actor id
- create should set updatedBy from actor id
- create should allow same content type key under different site keys
- create should reject duplicate key in same site
- create should allow global content type using `SiteKey.SYSTEM`
- findByKey should find by `siteKey + key`
- findByKey should return empty when siteKey differs
- activate should move draft to active and set updatedBy from actor id
- activate should update updatedAt
- activate should be idempotent when already active
- activate should fail when archived
- archive should move draft to archived and set updatedBy from actor id
- archive should move active to archived and set updatedBy from actor id
- archive should be idempotent when already archived
- missing content type should throw `NotFoundException` for activate
- missing content type should throw `NotFoundException` for archive
- required arguments should not accept null

## 11. Runtime

If `CodexRuntime` already exposes `ContentTypeService`, update wiring to use the new repository/service signatures.

If `CodexRuntime` does not expose `ContentTypeService` yet, do not add it unless all content type pieces are already stable and adding it is straightforward.

Do not add content type event publishing in this task.

## 12. Documentation

Update the Task7 notes or add a short note to docs explaining:

- Content types are scoped by `SiteKey`
- Global content types use `SiteKey.SYSTEM`
- `ContentTypeKey` alone is not globally unique
- logical identity is `siteKey + contentTypeKey`
- ownership metadata is stored as `ActorId`
- versioning and composition are still future work
- treeable/resource-path support is still future work

## Constraints

- Follow CLAUDE.md conventions.
- Follow the style of the existing Site implementation.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No content item implementation.
- No content type events.
- No event publishing wrapper for content types.
- No TreeableResource implementation.
- No CodexResourcePath implementation.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable classes.
- Run the smallest relevant Maven test or compile command after implementation.