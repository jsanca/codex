# Task11: ContentType Fields Foundation

## Objective

Add the first schema-field foundation to `ContentType`.

A `ContentType` should be able to hold a set of fields indexed by `FieldKey`.

This task introduces field management at the `ContentTypeService` level, but does not implement content items, content type versioning, field events, validation engine, rendering, persistence, or UI behavior.

The goal is to make a content type contain real schema information while keeping the model simple and future-compatible with `ContentTypeVersion`.

## Architectural direction

Fields are always accessed by key.

Use:

`Map<FieldKey, Field> fields`

instead of:

`List<Field> fields`

Reason:

- field lookup is naturally key-based
- duplicate field keys must be impossible
- content item validation will need fast access by `FieldKey`
- indexing/search configuration is field-key driven
- future schema snapshots can preserve the same structure

For now, fields live directly inside `ContentType`.

Future work may extract fields into `ContentTypeVersion`.

This task should treat `ContentType.fields` as the current draft schema of the content type.

## Scope

Implement:

- `Map<FieldKey, Field> fields` in `ContentType`
- `AddContentTypeFieldCommand`
- `RemoveContentTypeFieldCommand`
- `ContentTypeService.addField(...)`
- `ContentTypeService.removeField(...)`
- implementation in `CodexContentTypeService`
- semantic exceptions for duplicate/missing fields
- tests

Do not implement:

- ContentItem
- ContentRevision
- ContentTypeVersion
- field events
- content type field event publishing
- field inheritance
- field groups
- composition/mixins
- validation engine for content items
- rendering/UI concerns
- persistence
- Spring integration
- permission service
- JavaScript extension system
- TreeableResource
- CodexResourcePath

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.entity.ContentType`
- `codex.codex.api.model.entity.Field`
- `codex.codex.api.model.identity.FieldKey`
- `codex.codex.api.model.command.AddContentTypeFieldCommand`
- `codex.codex.api.model.command.RemoveContentTypeFieldCommand`
- `codex.codex.api.model.service.ContentTypeService`
- `codex.codex.internal.service.CodexContentTypeService`
- `codex.codex.internal.service.ContentTypeFieldAlreadyExistsException`
- `codex.codex.internal.service.ContentTypeFieldNotFoundException`

## 1. Refine Field if needed

Review the existing `Field` model before integrating it into `ContentType`.

Make only minimal corrections needed for safety and consistency.

Requirements:

- `Field.key()` must not be null
- `Field.type()` must not be null
- `displayName` should be trimmed if present
- constraints should be defensively copied
- settings should be defensively copied
- existing boolean flags should continue to work
- builder/copy pattern should continue to work if already present

Do not redesign `Field` in this task.

Do not add new field types in this task.

Do not add a validation engine in this task.

## 2. Update ContentType

Update `ContentType` to include fields.

Add field:

`Map<FieldKey, Field> fields`

Requirements:

- import `FieldKey`
- import `Field`
- validate `fields`
- if `fields` is null, default to `Map.of()`
- defensively copy fields with `Map.copyOf(fields)`
- builder must support `fields`
- `copyOf(...)` must preserve fields
- `toString()` should include field count, not necessarily full field details

Suggested `toString()` style:

`", fields=" + fields.size()`

Do not expose a mutable map.

Do not allow duplicate keys.

Because fields are stored in a `Map`, duplicate keys are naturally impossible in the final structure.

## 3. Field identity rule

The map key must match the field key.

When adding or constructing fields, ensure:

`mapKey.equals(field.key())`

If a command or builder attempts to associate a field under a different key than `field.key()`, throw `IllegalArgumentException`.

This prevents inconsistent schemas such as:

`fields["title"] = Field(key = "body")`

For regular `ContentType` construction, this check should happen in the canonical constructor.

## 4. Add AddContentTypeFieldCommand

Create:

`codex.codex.api.model.command.AddContentTypeFieldCommand`

Fields:

- `SiteKey siteKey`
- `ContentTypeKey contentTypeKey`
- `Field field`

Requirements:

- validate `siteKey` non-null
- validate `contentTypeKey` non-null
- validate `field` non-null
- provide typed factory:

`of(SiteKey siteKey, ContentTypeKey contentTypeKey, Field field)`

- provide string convenience factory if consistent with existing command style:

`of(String siteKey, String contentTypeKey, Field field)`

Do not include `Actor` in the command.

The actor is passed separately to the service.

## 5. Add RemoveContentTypeFieldCommand

Create:

`codex.codex.api.model.command.RemoveContentTypeFieldCommand`

Fields:

- `SiteKey siteKey`
- `ContentTypeKey contentTypeKey`
- `FieldKey fieldKey`

Requirements:

- validate `siteKey` non-null
- validate `contentTypeKey` non-null
- validate `fieldKey` non-null
- provide typed factory:

`of(SiteKey siteKey, ContentTypeKey contentTypeKey, FieldKey fieldKey)`

- provide string convenience factory if consistent with existing command style:

`of(String siteKey, String contentTypeKey, String fieldKey)`

Do not include `Actor` in the command.

## 6. Update ContentTypeService

Add methods:

`ContentType addField(AddContentTypeFieldCommand command, Actor actor)`

`ContentType removeField(RemoveContentTypeFieldCommand command, Actor actor)`

Requirements:

- service remains actor-aware
- use `Actor`, not Spring Security, Principal, JWT, Auth0, or framework concepts
- do not add permission service yet
- do not add event publishing in this task

The service should still expose existing methods:

- create
- activate
- archive
- findByKey
- findBySiteKey if already present
- findAll

## 7. Add field exceptions

Create semantic exceptions if they do not already exist.

Suggested exceptions:

- `ContentTypeFieldAlreadyExistsException`
- `ContentTypeFieldNotFoundException`

Location:

`codex.codex.internal.service`

Requirements:

- runtime exceptions
- simple and semantic
- include useful message
- include the relevant `ContentType` or key if consistent with existing exception style
- do not create a large exception hierarchy unless already present

Example messages:

`Field title already exists in content type blog-post`

`Field summary does not exist in content type blog-post`

## 8. Update CodexContentTypeService

Implement `addField` and `removeField`.

### addField

Behavior:

- require non-null command
- require non-null actor
- load content type by `command.siteKey()` and `command.contentTypeKey()`
- if missing, throw `NotFoundException`
- validate content type can be modified
- if field key already exists, throw `ContentTypeFieldAlreadyExistsException`
- create a new map from existing fields
- add the new field using `field.key()` as the map key
- save a copied content type with:
    - updated fields
    - `updatedBy = actor.id()`
    - `updatedAt = clock.instant()`
- return saved content type

### removeField

Behavior:

- require non-null command
- require non-null actor
- load content type by `command.siteKey()` and `command.contentTypeKey()`
- if missing, throw `NotFoundException`
- validate content type can be modified
- if field key does not exist, throw `ContentTypeFieldNotFoundException`
- create a new map from existing fields
- remove the field
- save a copied content type with:
    - updated fields
    - `updatedBy = actor.id()`
    - `updatedAt = clock.instant()`
- return saved content type

## 9. Field modification rules

For this task, allow field modifications only while the content type is `DRAFT`.

Rules:

- `DRAFT`: add/remove fields allowed
- `ACTIVE`: add/remove fields rejected
- `ARCHIVED`: add/remove fields rejected

Rejected modifications should throw a semantic exception.

Suggested exception:

`InvalidContentTypeSchemaModificationException`

Location:

`codex.codex.internal.service`

Example message:

`Cannot modify fields for content type blog-post while status is ACTIVE`

Reason:

Changing fields on an active content type implies schema versioning, which is future work.

This task should not implement versioning.

## 10. Validation pipeline

If `CodexContentTypeService` already has a validation helper, extend it.

If not, add small private validation methods.

Do not introduce a full validation framework in this task.

Suggested private methods:

- `findRequired(...)`
- `validateSchemaCanBeModified(...)`
- `validateFieldDoesNotExist(...)`
- `validateFieldExists(...)`

Keep implementation simple and readable.

Future work may introduce a generic validation pipeline similar to the planned site validation pipeline.

## 11. Idempotency

Do not make `addField` idempotent.

If the field already exists, throw `ContentTypeFieldAlreadyExistsException`.

Do not make `removeField` idempotent.

If the field does not exist, throw `ContentTypeFieldNotFoundException`.

Reason:

Field schema changes are structural operations and should be explicit.

## 12. Events

Do not add events in this task.

Do not modify `EventPublishingContentTypeService` in this task.

Future events may include:

- `ContentTypeFieldAddedEvent`
- `ContentTypeFieldRemovedEvent`

Those will be added in a later task if needed.

## 13. Runtime

If `CodexRuntime` exposes `ContentTypeService`, it should continue to work after adding the new service methods.

No new runtime wiring should be needed.

Do not add additional dispatchers or executors.

Do not add content type field event publishing in runtime.

## 14. Tests

Add or update plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repository and real service.

### Field tests

Add or update tests for:

- `Field` rejects null key
- `Field` rejects null type
- `Field` trims displayName if present
- constraints are defensively copied
- settings are defensively copied

Only add these if not already covered.

### ContentType tests

Add or update tests for:

- fields default to empty map when null
- fields are defensively copied
- fields map is immutable from public accessor
- builder supports fields
- `copyOf(...)` preserves fields
- constructor rejects field map where map key differs from `field.key()`
- `toString()` does not dump full field details if field count style is used

### Command tests

Add tests for `AddContentTypeFieldCommand`:

- requires non-null siteKey
- requires non-null contentTypeKey
- requires non-null field
- typed factory works
- string factory works if implemented

Add tests for `RemoveContentTypeFieldCommand`:

- requires non-null siteKey
- requires non-null contentTypeKey
- requires non-null fieldKey
- typed factory works
- string factory works if implemented

### Repository tests

Repository behavior should still work with fields.

Add or update tests for:

- saving a content type with fields preserves fields
- finding a content type with fields returns fields

### Service tests

Add tests for `CodexContentTypeService.addField`:

- addField adds field to draft content type
- addField updates `updatedBy`
- addField updates `updatedAt`
- addField rejects duplicate field key
- addField throws `NotFoundException` when content type is missing
- addField rejects ACTIVE content type
- addField rejects ARCHIVED content type
- addField requires non-null command
- addField requires non-null actor

Add tests for `CodexContentTypeService.removeField`:

- removeField removes field from draft content type
- removeField updates `updatedBy`
- removeField updates `updatedAt`
- removeField rejects missing field key
- removeField throws `NotFoundException` when content type is missing
- removeField rejects ACTIVE content type
- removeField rejects ARCHIVED content type
- removeField requires non-null command
- removeField requires non-null actor

### Integration tests

If there is an existing `CodexRuntime` integration test for content types, add a small test showing:

- create content type
- add field
- remove field

Do not assert events for field operations in this task.

## 15. Documentation

Update or add a short note explaining:

- fields are stored as `Map<FieldKey, Field>`
- field lookup is key-based
- fields currently live directly inside `ContentType`
- fields represent the current draft schema
- field modification is only allowed while `ContentTypeStatus.DRAFT`
- active schema changes are future work and will require `ContentTypeVersion`
- field events are future work

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
- No content item implementation.
- No ContentTypeVersion implementation.
- No field events.
- No event publishing wrapper changes.
- No TreeableResource implementation.
- No CodexResourcePath implementation.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable classes.
- Run the smallest relevant Maven test or compile command after implementation.