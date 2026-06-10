# Task12: ContentType Version Foundation

## Objective

Align and implement the `ContentTypeVersion` foundation so content type versions become immutable published schema snapshots.

A `ContentType` owns the editable draft/current schema through its `fields` map.

A `ContentTypeVersion` represents a published snapshot of that schema at a specific version number.

This task prepares Codex for future `ContentItem` support, where content items should reference a stable content type version instead of a mutable content type schema.

## Architectural direction

Keep these concepts separate:

`ContentType`
- aggregate root
- scoped by `SiteKey`
- owns lifecycle: `DRAFT`, `ACTIVE`, `ARCHIVED`
- owns editable draft/current fields
- tracks latest published version metadata

`ContentTypeVersion`
- immutable schema snapshot
- scoped by the same `SiteKey` and `ContentTypeKey`
- references the parent `ContentTypeId`
- stores fields as `Map<FieldKey, Field>`
- is created when a content type is activated/published
- can later be referenced by `ContentItem`

Do not model draft versions in `ContentTypeVersion`.

Draft schema lives in `ContentType.fields`.

## Scope

Implement:

- align `ContentTypeVersionStatus`
- align `ContentTypeVersion`
- add or update `ContentTypeVersionId`
- add `ContentTypeVersionRepository`
- add `MemoryContentTypeVersionRepository`
- update `CodexContentTypeService.activate(...)` to create a version snapshot
- update `ContentType` with latest published version metadata if not already present
- update `CodexRuntime` wiring if needed
- tests

Do not implement:

- ContentItem
- ContentRevision
- schema migrations
- version diffing
- compatibility rules
- reopen draft workflow
- version events
- field events
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

- `codex.codex.api.model.entity.ContentType`
- `codex.codex.api.model.entity.ContentTypeVersion`
- `codex.codex.api.model.identity.ContentTypeVersionId`
- `codex.codex.api.model.value.ContentTypeVersionStatus`
- `codex.codex.internal.repository.ContentTypeVersionRepository`
- `codex.codex.internal.repository.MemoryContentTypeVersionRepository`
- `codex.codex.internal.service.CodexContentTypeService`
- existing content type tests

## 1. Update ContentTypeVersionStatus

Update `ContentTypeVersionStatus`.

Replace the old version status model:

`DRAFT`
`ACTIVE`
`DEPRECATED`
`ARCHIVED`

with:

`PUBLISHED`
`DEPRECATED`
`ARCHIVED`

Semantics:

`PUBLISHED`
- the schema version is published
- it can be referenced by new content items
- usually the latest published version for a content type

`DEPRECATED`
- the schema version is retained and may still be referenced by existing content items
- it should not be used for new content items

`ARCHIVED`
- the schema version is retained for history or compatibility
- it is not used for normal content creation

Do not keep `DRAFT` in `ContentTypeVersionStatus`.

Draft schema belongs to `ContentType.fields`.

Add JavaDoc to the enum and each value.

## 2. Update ContentTypeVersion

Update `ContentTypeVersion` to align with the current `ContentType` model.

The final record shape should be equivalent to:

`ContentTypeVersionId id`
`ContentTypeId contentTypeId`
`SiteKey siteKey`
`ContentTypeKey contentTypeKey`
`int version`
`Map<FieldKey, Field> fields`
`ContentTypeVersionStatus status`
`ActorId createdBy`
`Instant createdAt`

Requirements:

- `id` must not be null
- `contentTypeId` must not be null
- `siteKey` must not be null
- `contentTypeKey` must not be null
- `version` must be >= 1
- `fields` defaults to `Map.of()` if null
- `fields` must be defensively copied with `Map.copyOf(...)`
- map key must match `field.key()`
- `status` defaults to `ContentTypeVersionStatus.PUBLISHED` if null
- `createdBy` must not be null
- `createdAt` defaults to `Instant.now()` if null
- builder must support all fields
- `copyOf(...)` should exist and preserve all fields
- `toString()` should include:
    - id
    - contentTypeId
    - siteKey
    - contentTypeKey
    - version
    - field count or field keys
    - status
    - createdBy
    - createdAt

Do not use `List<Field>`.

Use `Map<FieldKey, Field>`.

## 3. ContentTypeVersionId

Review `ContentTypeVersionId`.

Requirements:

- use a Java record if consistent with existing identity style
- wrap a `String value`
- validate non-null
- validate non-blank
- provide `of(String value)`
- provide deterministic factory if consistent with current identity style

Suggested deterministic id format:

`content-type-version:{siteKey}:{contentTypeKey}:v{version}`

Example:

`content-type-version:system:blog-post:v1`

Do not use random UUIDs if the project is already using deterministic ids for similar identities.

## 4. Update ContentType with latest published version metadata

Update `ContentType` to track the latest published version.

Add fields if they do not already exist:

`ContentTypeVersionId latestPublishedVersionId`
`Integer latestPublishedVersion`

Requirements:

- both may be null while the content type is still `DRAFT` and has never been activated
- when set, `latestPublishedVersion` must be >= 1
- builder must support both fields
- `copyOf(...)` must preserve both fields
- `toString()` should include them
- creating a new content type should leave them null
- activating a content type should set both

Do not require a published version for `DRAFT`.

Do not require a published version for newly created content types.

## 5. Create ContentTypeVersionRepository

Create:

`codex.codex.internal.repository.ContentTypeVersionRepository`

Contract should be equivalent to:

`ContentTypeVersion save(ContentTypeVersion version)`

`Optional<ContentTypeVersion> findById(ContentTypeVersionId id)`

`Optional<ContentTypeVersion> findByContentTypeAndVersion(ContentTypeId contentTypeId, int version)`

`Optional<ContentTypeVersion> findLatestPublished(ContentTypeId contentTypeId)`

`List<ContentTypeVersion> findByContentType(ContentTypeId contentTypeId)`

Optional, only if simple and useful:

`List<ContentTypeVersion> findBySiteKeyAndContentTypeKey(SiteKey siteKey, ContentTypeKey contentTypeKey)`

Requirements:

- validate null arguments
- validate version number >= 1
- no Spring
- no persistence framework
- no generic query API

## 6. Create MemoryContentTypeVersionRepository

Create:

`codex.codex.internal.repository.MemoryContentTypeVersionRepository`

Requirements:

- implements `ContentTypeVersionRepository`
- stores versions in memory
- key by `ContentTypeVersionId`
- support lookup by `ContentTypeId + version`
- support latest published lookup
- `save(...)` returns saved version
- `findById(...)` returns optional
- `findByContentTypeAndVersion(...)` returns optional
- `findByContentType(...)` returns immutable snapshot
- `findLatestPublished(...)` returns the highest version with status `PUBLISHED`
- validate null arguments
- use simple collections
- no Spring
- no external infrastructure

If multiple versions are `PUBLISHED`, `findLatestPublished(...)` should return the highest version number.

Do not implement deprecation of previous versions automatically unless explicitly needed by service logic.

## 7. Update CodexContentTypeService constructor

Update `CodexContentTypeService` so it can create version snapshots.

Constructor should receive:

- `ContentTypeRepository`
- `ContentTypeVersionRepository`
- `Clock`
- identity generator for `ContentType` if already present
- identity generator or factory for `ContentTypeVersion` if needed

Validate all dependencies with `Objects.requireNonNull`.

If there is an existing constructor used by tests, keep a convenience constructor if useful, but ensure the main constructor accepts the version repository.

No Spring annotations.

## 8. Update activate behavior

Update `CodexContentTypeService.activate(...)`.

Current behavior likely changes `ContentTypeStatus.DRAFT` to `ACTIVE`.

New behavior:

When activating a `DRAFT` content type:

1. load content type by `siteKey + key`
2. validate it exists
3. validate it can be activated
4. compute next version number
5. create `ContentTypeVersion` snapshot from current `ContentType.fields`
6. save the version snapshot as `PUBLISHED`
7. save the content type as `ACTIVE`
8. update:
    - `latestPublishedVersionId`
    - `latestPublishedVersion`
    - `updatedBy = actor.id()`
    - `updatedAt = clock.instant()`
9. return updated content type

When activating an already `ACTIVE` content type:

- return existing content type
- do not create a new version
- do not update `updatedAt`
- do not update `updatedBy`

When activating an `ARCHIVED` content type:

- throw `InvalidContentTypeStatusTransitionException`
- do not create a version

## 9. Version number rule

For this task, version numbers are sequential per content type.

Rules:

- first activation creates version `1`
- next future activation after draft edits should create version `latestPublishedVersion + 1`
- if `latestPublishedVersion` is null, create version `1`
- if repository contains higher versions, prefer repository latest version + 1 if implemented safely

For this task, because reopen draft workflow is not implemented yet, the most important behavior is:

- first activation creates version `1`
- idempotent activation does not create version `2`

Do not implement reopen draft in this task.

## 10. ContentTypeVersion snapshot rule

The version snapshot must copy fields from `ContentType.fields`.

Requirements:

- use `Map.copyOf(...)` through `ContentTypeVersion`
- do not keep a mutable reference to the content type fields
- later changes to `ContentType.fields` must not mutate existing versions
- validate map key equals `field.key()`

This is critical because versions must be immutable schema snapshots.

## 11. Deprecation behavior

Do not automatically deprecate previous versions in this task unless the service already supports multiple published versions.

For now:

- version `1` is `PUBLISHED`
- idempotent activation does not create another version
- future version deprecation policy will be handled later

Future policy may be:

- when publishing v2, mark v1 as `DEPRECATED`
- keep old versions referencable by existing content items
- latest published version is used for new content items

Do not implement this policy yet.

## 12. Archive behavior

Do not change archive behavior unless necessary to keep latest published metadata consistent.

When archiving a content type:

- do not delete versions
- do not remove latest published version metadata
- do not archive content type versions automatically in this task

Future policy may decide whether archiving a content type should also archive or deprecate versions.

For now, versions remain historical snapshots.

## 13. Events

Do not add version-specific events in this task.

Do not modify `EventPublishingContentTypeService` unless compilation requires adapting to new constructor/service behavior.

Future events may include:

- `ContentTypeVersionPublishedEvent`
- `ContentTypeVersionDeprecatedEvent`
- `ContentTypeVersionArchivedEvent`

Existing `ContentTypeActivatedEvent` may later include version metadata, but do not require that in this task.

## 14. Runtime

Update `CodexRuntime` if it already wires `CodexContentTypeService`.

Add:

- `MemoryContentTypeVersionRepository`
- pass it into `CodexContentTypeService`

Do not create a separate runtime for versions.

Do not create a separate event dispatcher.

Do not create a separate executor.

## 15. Tests

Add or update plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repositories and real services.

### ContentTypeVersionStatus tests

Add tests or compile-level coverage ensuring:

- `PUBLISHED`
- `DEPRECATED`
- `ARCHIVED`

exist.

Remove expectations for:

- `DRAFT`
- `ACTIVE`

in version status tests.

### ContentTypeVersion tests

Add or update tests for:

- rejects null id
- rejects null contentTypeId
- rejects null siteKey
- rejects null contentTypeKey
- rejects version less than 1
- defaults fields to empty map
- defensively copies fields
- fields accessor is immutable
- rejects field map where key does not match `field.key()`
- defaults status to `PUBLISHED`
- rejects null createdBy
- defaults createdAt when null
- builder supports siteKey
- builder supports contentTypeKey
- builder supports fields map
- builder supports createdBy
- `copyOf(...)` preserves all fields

### ContentType tests

Add or update tests for:

- new content type may have null latestPublishedVersionId
- new content type may have null latestPublishedVersion
- rejects latestPublishedVersion less than 1 if set
- builder supports latestPublishedVersionId
- builder supports latestPublishedVersion
- `copyOf(...)` preserves latest published metadata

### MemoryContentTypeVersionRepository tests

Add tests for:

- save returns saved version
- findById returns saved version
- findById returns empty when missing
- findByContentTypeAndVersion returns saved version
- findByContentTypeAndVersion returns empty when missing
- findByContentType returns all versions for that content type
- findByContentType does not return versions for other content types
- findLatestPublished returns highest published version
- findLatestPublished ignores deprecated and archived versions
- required arguments reject null
- version less than 1 rejects where applicable

### CodexContentTypeService tests

Update service tests for activation:

- activating draft creates version 1
- activating draft sets latestPublishedVersion to 1
- activating draft sets latestPublishedVersionId
- activating draft stores version snapshot with content type fields
- activating draft stores version with status `PUBLISHED`
- activating draft stores version with createdBy equal to actor id
- activating already active content type does not create another version
- activating archived content type does not create version
- activating missing content type does not create version
- archive does not delete versions
- archive preserves latest published version metadata

### Snapshot immutability test

Add a test proving version fields are a snapshot.

Suggested scenario:

1. create content type
2. add field `title`
3. activate content type
4. retrieve version `1`
5. verify version has `title`
6. mutate content type draft fields later if possible within current rules, or directly construct a changed copy for repository-level test
7. verify version `1` fields did not change

If current service rules prevent modifying active content types, do this as a `ContentTypeVersion` constructor/repository test by mutating the original map after construction and verifying the version does not change.

### Runtime tests

If `CodexRuntime` wires content type service:

- create runtime
- create content type
- add field
- activate content type
- assert no wiring errors
- optionally verify version repository only if accessible

Do not expose version repository publicly only for this test unless already planned.

## 16. Documentation

Add or update a short note explaining:

- `ContentType.fields` is the editable draft/current schema
- `ContentTypeVersion.fields` is an immutable published schema snapshot
- `ContentTypeVersionStatus` uses `PUBLISHED`, `DEPRECATED`, `ARCHIVED`
- draft versions are not modeled in this task
- first activation creates version `1`
- idempotent activation does not create a new version
- old versions remain for future content item compatibility
- reopen draft and publish v2 are future work
- content items will later reference a content type version

## Constraints

- Follow CLAUDE.md conventions.
- Follow the style of the existing ContentType implementation.
- Use `Map<FieldKey, Field>`, not `List<Field>`.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No Custos implementation.
- No ContentItem implementation.
- No ContentRevision implementation.
- No reopen draft workflow.
- No version events.
- No field events.
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