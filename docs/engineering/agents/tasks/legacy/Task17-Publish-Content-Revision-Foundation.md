# Task17: Publish Content Revision Foundation

## Objective

Implement the first publishing foundation for content items.

A `ContentItem` is the stable logical identity of content.

A `ContentRevision` is the immutable snapshot of content values.

Publishing should not copy values into `ContentItem`.

Publishing should mark a working revision as published and update the content item pointers:

- `ContentItem.currentPublishedRevisionId`
- `ContentItem.status`
- `ContentItem.updatedBy`
- `ContentItem.updatedAt`

This task introduces the first publish operation but does not implement workflow, approvals, rollback, unpublish, revision diffing, search indexing, audit, cache invalidation, or events.

## Architectural direction

Publishing is pointer-based.

Do not move content values into `ContentItem`.

Do not duplicate revision values.

The publish operation should conceptually do this:

1. load the content item
2. load its current working revision
3. validate that the revision can be published
4. save a published revision state
5. update the content item to point to the published revision
6. return the updated content item

For this task, publishing the current working revision is enough.

Future work may support:

- publishing a specific revision
- workflow approvals
- scheduled publishing
- unpublish
- rollback to previous revision
- creating a new working revision after publish
- revision comparison
- revision locking
- publish events

## Scope

Implement:

- `PublishContentItemCommand`
- `ContentItemService.publish(...)`
- `CodexContentItemService.publish(...)`
- semantic exceptions if needed
- repository support if needed
- tests

Do not implement:

- ContentItemPublishedEvent
- EventPublishingContentItemService publish support
- workflow
- approvals
- unpublish
- rollback
- edit after publish
- new working revision after publish
- revision diffing
- revision comments
- revision locking
- search indexing
- audit
- cache invalidation
- permissions
- Custos
- Spring integration
- persistence
- TreeableResource
- CodexResourcePath
- JavaScript extensions

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.command.PublishContentItemCommand`
- `codex.codex.api.model.service.ContentItemService`
- `codex.codex.internal.service.CodexContentItemService`
- `codex.codex.internal.service.InvalidContentItemPublishException`
- existing content item tests
- existing runtime/integration tests

## 1. Create PublishContentItemCommand

Create:

`codex.codex.api.model.command.PublishContentItemCommand`

Fields:

- `SiteKey siteKey`
- `ContentTypeKey contentTypeKey`
- `ContentItemKey key`

Requirements:

- validate `siteKey` non-null
- validate `contentTypeKey` non-null
- validate `key` non-null
- provide typed factory:

`of(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key)`

- provide string convenience factory if consistent with existing command style:

`of(String siteKey, String contentTypeKey, String itemKey)`

Do not include `Actor` in the command.

The actor is passed separately to the service.

Do not include `ContentRevisionId` in this command.

For this task, publish always uses the content item’s current working revision.

## 2. Update ContentItemService

Add method:

`ContentItem publish(PublishContentItemCommand command, Actor actor)`

Requirements:

- service remains actor-aware
- use `codex.fundamentum.api.model.Actor`
- do not use Spring Security, Principal, JWT, Auth0, or framework concepts
- do not add permission service yet
- do not add event publishing in this task

Existing methods should remain unchanged.

## 3. Update ForwardingContentItemService if it exists

If `ForwardingContentItemService` exists, add default forwarding for:

`publish(PublishContentItemCommand command, Actor actor)`

Requirements:

- default method forwards to `getDelegate().publish(command, actor)`
- no state
- no event publishing in this task

Do not update `EventPublishingContentItemService` to publish events in this task.

It may simply inherit the forwarding behavior for `publish`.

## 4. Add publish exception

Create semantic exception if needed:

`codex.codex.internal.service.InvalidContentItemPublishException`

Requirements:

- runtime exception
- simple and semantic
- useful message
- may include `ContentItem` or relevant keys if consistent with existing exception style
- do not create a large exception hierarchy

Example messages:

- `Cannot publish content item welcome-to-codex because it has no working revision`
- `Cannot publish content item welcome-to-codex because the working revision is not WORKING`
- `Cannot publish archived content item welcome-to-codex`

## 5. Update CodexContentItemService constructor only if needed

Use existing dependencies:

- `ContentItemRepository`
- `ContentRevisionRepository`
- `ContentTypeRepository`
- `ContentTypeVersionRepository`
- `Clock`

No new dependency should be needed for publish.

Do not add event dispatcher.

Do not add transaction annotations.

## 6. Implement publish behavior

Implement:

`ContentItem publish(PublishContentItemCommand command, Actor actor)`

Behavior:

1. require non-null command
2. require non-null actor
3. load content item by:
    - `command.siteKey()`
    - `command.contentTypeKey()`
    - `command.key()`
4. if missing, throw `NotFoundException`
5. validate content item can be published
6. load current working revision by `contentItem.currentWorkingRevisionId()`
7. if current working revision id is null, throw `InvalidContentItemPublishException`
8. if working revision is missing, throw `NotFoundException`
9. validate revision can be published
10. create a copied revision with:
    - same id
    - same content item id
    - same siteKey
    - same contentTypeKey
    - same contentTypeVersionId
    - same contentItemKey
    - same revisionNumber
    - same values
    - status `ContentRevisionStatus.PUBLISHED`
    - same createdBy
    - same createdAt
11. save the published revision
12. save a copied content item with:
    - status `ContentItemStatus.PUBLISHED`
    - currentPublishedRevisionId = published revision id
    - currentWorkingRevisionId = published revision id for this first foundation
    - updatedBy = actor.id()
    - updatedAt = clock.instant()
13. return updated content item

Important:

This operation is multi-write.

It must eventually run inside a transaction boundary.

For now, keep the implementation straightforward and rely on future transactional service wrapping.

## 7. Publish validation rules

### ContentItem status validation

Rules for this task:

- `DRAFT`: publish allowed if it has a valid working revision
- `PUBLISHED`: publishing the same current working revision is idempotent
- `ARCHIVED`: publish rejected

If item is `ARCHIVED`, throw `InvalidContentItemPublishException`.

### Revision validation

The current working revision must exist.

The current working revision must have status `WORKING` or `PUBLISHED`.

Rules:

- `WORKING`: publish allowed
- `PUBLISHED`: publish is idempotent if it is already the current published revision
- `ARCHIVED`: publish rejected

If the current working revision has status `ARCHIVED`, throw `InvalidContentItemPublishException`.

### Idempotency

Publishing an already published item should be idempotent if:

- item status is `PUBLISHED`
- `currentWorkingRevisionId` is not null
- `currentPublishedRevisionId` equals `currentWorkingRevisionId`
- current working revision status is `PUBLISHED`

In that case:

- return the existing item
- do not save a new revision
- do not update `updatedAt`
- do not update `updatedBy`

Do not create a new revision during publish.

Do not create a new working revision after publish in this task.

Future edit operations will create new working revisions.

## 8. Revision pointer rule

For this first foundation, after publishing:

`currentPublishedRevisionId = currentWorkingRevisionId`

and the revision itself is marked:

`ContentRevisionStatus.PUBLISHED`

The item remains pointing to the same revision as both working and published.

This is acceptable for the first publish foundation.

Future edit support will create a new `WORKING` revision that differs from the published revision.

Example future state:

- `currentPublishedRevisionId = r1`
- `currentWorkingRevisionId = r2`

Do not implement that future state in this task.

## 9. Repository behavior

No new repository methods should be required if the service can use:

- `ContentItemRepository.findByKey(...)`
- `ContentItemRepository.save(...)`
- `ContentRevisionRepository.findById(...)`
- `ContentRevisionRepository.save(...)`

If a needed method is missing, add the smallest explicit method.

Do not add generic query APIs.

Do not add delete.

Do not make repositories transactional in this task.

## 10. Event behavior

Do not add content item publish events in this task.

Do not create:

- `ContentItemPublishedEvent`
- `ContentRevisionPublishedEvent`

Do not modify `EventPublishingContentItemService` to dispatch publish events.

If `ForwardingContentItemService` is updated, `EventPublishingContentItemService` can forward publish to the delegate without publishing an event.

Publish events will be handled in a later task.

## 11. Tests

Add or update plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repositories and real services.

### PublishContentItemCommand tests

Add tests for:

- rejects null siteKey
- rejects null contentTypeKey
- rejects null key
- typed factory works
- string factory works if implemented

### ContentItemService / CodexContentItemService tests

Set up tests using real in-memory repositories.

Test fixtures should create:

- an ACTIVE content type
- a PUBLISHED content type version
- a DRAFT content item
- a WORKING content revision

Add tests for:

- publish should update item status to `PUBLISHED`
- publish should set `currentPublishedRevisionId`
- publish should keep `currentWorkingRevisionId` equal to the published revision id
- publish should update `updatedBy`
- publish should update `updatedAt`
- publish should mark working revision as `PUBLISHED`
- publish should preserve revision values
- publish should preserve revision number
- publish should preserve revision createdBy
- publish should preserve revision createdAt
- publish should return existing item when already published and pointers match
- idempotent publish should not update `updatedAt`
- idempotent publish should not update `updatedBy`
- publish should throw `NotFoundException` when item is missing
- publish should throw `InvalidContentItemPublishException` when item has no working revision
- publish should throw `NotFoundException` when working revision is missing
- publish should throw `InvalidContentItemPublishException` when item is `ARCHIVED`
- publish should throw `InvalidContentItemPublishException` when working revision is `ARCHIVED`
- required arguments reject null

### Repository tests

No new repository behavior is required unless methods are added.

If methods are added, test them.

### Event tests

Update event publishing tests only if required by interface changes.

Verify that `EventPublishingContentItemService` forwards `publish` through `ForwardingContentItemService`.

Do not expect publish events.

### Runtime / integration test

If `CodexRuntime` exposes `ContentItemService`, add or update an integration test:

Flow:

1. create site
2. create content type
3. add required field
4. activate content type
5. create content item
6. publish content item

Assert:

- item status is `PUBLISHED`
- currentPublishedRevisionId is not null
- currentPublishedRevisionId equals currentWorkingRevisionId
- no publish event is expected in this task

If content item create events already exist, do not change their behavior.

## 12. Transaction note

Publishing is a multi-write operation:

- revision save
- item save

This task does not implement real transaction management.

Add a comment or documentation note if useful:

`publish should be executed inside a transaction boundary by the runtime/service wrapper in the future.`

Do not implement transaction wrapper in this task.

## 13. Documentation

Add or update a short note explaining:

- publishing is pointer-based
- values remain in `ContentRevision`
- `ContentItem` stores revision pointers
- first publish sets both working and published revision pointers to the same revision
- future edits will create a new working revision
- publish events are future work
- workflow is future work
- transaction management is future work

## Constraints

- Follow CLAUDE.md conventions.
- Follow the style of existing ContentItem and ContentRevision implementations.
- Keep ContentItem and ContentRevision responsibilities separate.
- Do not move values back into ContentItem.
- Do not create new revisions during publish.
- Do not implement publish events.
- Do not implement workflow.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No Custos implementation.
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
- Run the smallest relevant Maven test command after implementation.