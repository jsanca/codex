# Task16: Content Authoring End-to-End Integration Test

## Objective

Add an end-to-end integration test for the current Codex authoring flow.

This task should not add new production features.

The goal is to verify that the existing in-memory runtime can execute a complete content authoring flow using real components:

- `CodexRuntime`
- `TransactionContext`
- `SiteService`
- `ContentTypeService`
- `ContentItemService`
- in-memory repositories
- content type versioning
- content revision creation
- transaction-aware event dispatching

This is still a JUnit 5 test.

It is called end-to-end because it exercises the full internal Codex domain/runtime flow, not because it uses UI, browser automation, HTTP, Spring, or Selenium.

## Architectural direction

This test should prove that Codex already behaves like a small CMS engine:

1. create a site
2. create a content type
3. add fields to the content type
4. activate the content type
5. create a content item
6. create the first working content revision
7. dispatch events only after transaction commit

The test should use real production components through `CodexRuntime.inMemory()`.

Do not use mocks.

Do not wire services manually unless `CodexRuntime` is missing required access.

## Scope

Implement:

- one new JUnit 5 integration test class
- one or more tests covering the full authoring flow
- helper methods inside the test class if useful

Do not implement:

- new production features
- publish/unpublish
- workflow
- revision rollback
- search indexing
- audit
- cache invalidation
- permissions
- Custos
- Spring integration
- REST
- persistence
- TreeableResource
- CodexResourcePath
- Selenium/browser tests

## Suggested test class

Create:

`codex.codex.internal.runtime.ContentAuthoringFlowIntegrationTest`

or, if integration tests are grouped under service tests:

`codex.codex.internal.service.ContentAuthoringFlowIntegrationTest`

Prefer the runtime package if `CodexRuntime` is the entry point.

## Test style

Use:

- JUnit 5
- no Spring
- no Mockito
- no external infrastructure
- `CodexRuntime.inMemory()`
- `TransactionContext.runInTransaction(...)`
- `Actor.system("integration-test")`
- deterministic keys
- real services from runtime

Example structure:

`try (CodexRuntime runtime = CodexRuntime.inMemory()) { ... }`

The test should run with the normal Maven test lifecycle.

## Required flow test

Create a test named:

`full authoring flow creates content type version content item and working revision`

The test should perform this flow:

### 1. Create a site

Use `SiteService`.

Create a site with a deterministic key, for example:

`authoring-site`

Assert:

- site is created
- site key is correct
- site starts in the expected default status

If the current site model defaults to `STARTED`, assert that.

If the current create command requires display name, use:

`Authoring Site`

### 2. Create a content type

Use `ContentTypeService`.

Create a content type under the site key:

- siteKey: `authoring-site`
- contentTypeKey: `blog-post`
- displayName: `Blog Post`

Assert:

- content type is created
- status is `DRAFT`
- latest published version id is null
- latest published version number is null

### 3. Add fields

Add at least two fields:

Required field:

- fieldKey: `title`
- type: use the simplest text/string field type currently available
- required: true
- displayName: `Title`

Optional field:

- fieldKey: `summary`
- type: same simple text/string field type if available
- required: false
- displayName: `Summary`

Assert:

- content type has both fields
- fields are stored by `FieldKey`
- required flag is preserved for `title`

Use the existing `Field` builder/factory style.

Do not introduce new field types.

### 4. Activate the content type

Activate the content type.

Activation should:

- move the content type to `ACTIVE`
- create a `ContentTypeVersion`
- set latest published version metadata on `ContentType`

Assert:

- content type status is `ACTIVE`
- latest published version id is not null
- latest published version number is `1`

If `CodexRuntime` exposes no version repository, do not force exposing it just for this test.

Instead, continue by creating a content item, which indirectly proves the version exists because `ContentItemService` must validate against it.

If version repository access already exists in tests, also assert:

- version `1` exists
- version status is `PUBLISHED`
- version fields contain `title` and `summary`

### 5. Create a content item

Use `ContentItemService`.

Create a content item:

- siteKey: `authoring-site`
- contentTypeKey: `blog-post`
- contentItemKey: `welcome-to-codex`
- values:
    - `title` -> `Welcome to Codex`
    - `summary` -> `A first end-to-end content item`

Assert:

- item is created
- item status is `DRAFT`
- item key is correct
- item siteKey is correct
- item contentTypeKey is correct
- item contentTypeVersionId equals latest published version id from the content type
- item currentWorkingRevisionId is not null
- item currentPublishedRevisionId is null

### 6. Assert working revision

If `CodexRuntime` or test fixtures expose `ContentRevisionRepository`, assert directly:

- current working revision exists
- revision status is `WORKING`
- revision number is `1`
- revision contentItemId equals item id
- revision contentTypeVersionId equals item contentTypeVersionId
- revision values contain:
    - `title` -> `Welcome to Codex`
    - `summary` -> `A first end-to-end content item`

If revision repository is not accessible, do not expose it publicly only for this test unless there is already a test-only fixture pattern.

In that case, assert that the item has `currentWorkingRevisionId` and rely on service-level tests for revision content validation.

## Transaction behavior test

Add a second test named:

`full authoring flow dispatches events only after commit`

This test should use `TransactionContext.runInTransaction(...)`.

Inside the transaction:

1. create site
2. create content type
3. add field
4. activate content type
5. create content item
6. before returning from the transaction, assert recorded events are empty

After the transaction commits:

- assert events were recorded
- assert at least:
    - `SiteCreatedEvent`
    - `ContentTypeCreatedEvent`
    - `ContentTypeActivatedEvent`
    - `ContentItemCreatedEvent`

If field events are not implemented, do not expect field events.

If content type version events are not implemented, do not expect version events.

If content revision events are not implemented, do not expect revision events.

Important:

The test should not depend on exact event list order unless the event dispatcher guarantees order.

If order is not guaranteed, assert by event type presence.

## Rollback behavior test

Add a third test named:

`full authoring flow rollback dispatches no events`

Inside `TransactionContext.runInTransaction(...)`:

1. create site
2. create content type
3. add field
4. activate content type
5. create content item
6. throw `RuntimeException("forced rollback")`

After catching the exception:

- assert no events were recorded

This test validates the existing deferred event pipeline across the full authoring flow.

Note:

This test only validates event rollback behavior.

If in-memory repositories are not transactional yet, do not assert that repository data rolled back.

That belongs to a future transactional repository/unit-of-work task.

## Optional validation test

Add one optional test if time allows:

`content item creation fails when required field is missing in full flow`

Flow:

1. create site
2. create content type
3. add required `title` field
4. activate content type
5. attempt to create content item without `title`

Assert:

- semantic field validation exception is thrown
- no `ContentItemCreatedEvent` is dispatched

Use the existing exception type from `CodexContentItemService`.

Do not add new validation behavior in this task.

## Event assertions

Prefer helper methods inside the test:

- `assertEventRecorded(Class<? extends CodexEvent> type)`
- `assertNoEvents(CodexRuntime runtime)` if runtime exposes recorded events
- `containsEvent(Class<? extends CodexEvent> type)`

Do not assert exact event ordering unless the implementation explicitly guarantees it.

Do not assert event payload values exhaustively in this end-to-end test.

Payloads should already be covered by unit/integration tests for each event publishing service.

For this test, focus on proving the flow emits the expected event types after commit.

## Runtime access

Use existing `CodexRuntime` APIs.

If the runtime already exposes recorded events for tests, use that.

If it does not, prefer the existing `TestCodexContext` fixture if available.

Do not expose internal repositories publicly only to satisfy this test.

If repository access is necessary for asserting revisions, prefer package-private test support or existing test fixture patterns.

Keep runtime public API clean.

## Documentation

Add a short note to the test JavaDoc explaining:

- this is an internal end-to-end integration test
- it exercises the in-memory Codex runtime
- it does not use HTTP, UI, Spring, or external infrastructure
- it verifies the authoring model from Site to ContentRevision
- transactional rollback assertions are limited to event dispatching, not repository rollback

## Constraints

- Follow CLAUDE.md conventions.
- Use JUnit 5.
- No Spring.
- No Mockito.
- No Selenium.
- No HTTP.
- No external infrastructure.
- No new production features.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer readable helper methods over duplicated setup.
- Run the smallest relevant Maven test command after implementation.