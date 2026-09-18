# Task7: Content Types Foundation

## Objective

Implement the first foundation for Content Types in `codex-codex`.

This task introduces the initial domain model, repository, in-memory repository, service contract, and internal service implementation for managing content types.

A Content Type defines the schema for future content items. This task should not implement content items yet.

The goal is to create a clean, minimal, framework-agnostic foundation that follows the same architectural style already used for `Site`.

## Important architectural direction

`ContentTypeService` must not be a simple mirror of `ContentTypeRepository`.

The repository owns storage concerns.

The service owns application semantics such as:

- creating a content type
- finding content types
- validating duplicate keys
- preparing the model for future versioning
- preparing the model for future events
- preparing the model for future field/schema operations

Do not implement field mutation operations yet unless explicitly required for compilation.

## Location

Use these packages:

```text
codex.codex.api.model.entity
codex.codex.api.model.identity
codex.codex.api.model.command
codex.codex.api.model.service
codex.codex.internal.repository
codex.codex.internal.service
```

Do not introduce Spring, JPA, REST controllers, persistence annotations, or external infrastructure.

# Domain model

Create the following minimal model if it does not already exist.

ContentTypeId

Location: codex.codex.api.model.identity.ContentTypeId

Requirements:

1. Use a Java record
2. Wrap a String value
3. Validate non-null
4. Validate non-blank
5. Provide a static factory method
6. Prefer a deterministic or generated factory if the project already has an identity pattern

Example shape:
```
public record ContentTypeId(String value) {
    public ContentTypeId {
        // validation
    }

    public static ContentTypeId of(String value) { ... }
}
```

# ContentTypeKey

Location: codex.codex.api.model.identity.ContentTypeKey

Requirements:

1) Use a Java record
2) Wrap a String value
3) Validate non-null
4) Trim and normalize to lowercase
5) Validate non-blank
6) Use a safe key format similar to SiteKey
7) Provide a static factory method

This key represents the stable human-friendly identity of the content type.

Examples:
```
blog-post
product
landing-page
author-profile
```

# ContentTypeStatus
Location: codex.codex.api.model.value.ContentTypeStatus

Initial values:
```
DRAFT,
ACTIVE,
ARCHIVED
```

Semantics:

- DRAFT: schema is being defined
- ACTIVE: schema can be used by content items
- ARCHIVED: schema is retired from normal use

# ContentType
Location: codex.codex.api.model.entity.ContentType

Requirements:

- Use a Java record unless this conflicts with the existing style
- Fields:
    -- ContentTypeId id
    -- ContentTypeKey key
    -- String displayName
    -- ContentTypeStatus status
    -- Instant createdAt
    -- Instant updatedAt
    -- Validate required fields
    -- Trim displayName
    -- Default status to DRAFT if null
    -- Default timestamps if appropriate, following the existing project style
    -- Provide builder/copy pattern if the existing entities use it

Do not implement fields/schema definitions in this task unless they already exist and can be safely reused.

# Commands

Create these commands:
```
CreateContentTypeCommand
ActivateContentTypeCommand
ArchiveContentTypeCommand
```

Location: codex.codex.api.model.command

## CreateContentTypeCommand

Fields:
```
ContentTypeKey key
String displayName
```

Requirements:

- Validate non-null key
- Validate non-null/non-blank displayName
- Provide static factory method of(...)

# ActivateContentTypeCommand

Fields:
```
ContentTypeKey key
```

Requirements:

- Validate non-null key
- Provide static factory method of(...)

## ArchiveContentTypeCommand

Fields:
```
ContentTypeKey key
```

Requirements:

- Validate non-null key
- Provide static factory method of(...)

# Repository

Create: codex.codex.internal.repository.ContentTypeRepository

Contract:
```
public interface ContentTypeRepository {

    ContentType save(ContentType contentType);

    Optional<ContentType> findByKey(ContentTypeKey key);

    boolean existsByKey(ContentTypeKey key);

    List<ContentType> findAll();
}
```

Constraints:

- Repository methods use ContentTypeKey, not raw String
- No Spring
- No persistence framework
- No generic iterator/filter API yet

# In-memory repository

Create: codex.codex.internal.repository.MemoryContentTypeRepository

Requirements:

- Implements ContentTypeRepository
- Uses an in-memory map keyed by ContentTypeKey
- Validates null arguments with Objects.requireNonNull
- save(...) returns the saved ContentType
- findAll() returns an immutable snapshot
- Preserve behavior style already used by MemorySiteRepository

# Service contract

Create: codex.codex.api.model.service.ContentTypeService

Contract: 
```
public interface ContentTypeService {

    ContentType create(CreateContentTypeCommand command, Actor actor);

    ContentType activate(ActivateContentTypeCommand command, Actor actor);

    ContentType archive(ArchiveContentTypeCommand command, Actor actor);

    Optional<ContentType> findByKey(ContentTypeKey key, Actor actor);

    List<ContentType> findAll(Actor actor);
}
```

Important:

- This service is actor-aware.
- Use codex.fundamentum.api.model.Actor.
- Do not use Spring Security, Principal, Auth0, JWT, or framework concepts.
- Do not add permission services yet.

# Internal service implementation

Create: codex.codex.internal.service.CodexContentTypeService

Requirements:

- final
- Implements ContentTypeService
- Constructor receives:
  -- ContentTypeRepository
  -- Clock
  -- optional identity generator if needed by the existing style
- Validate constructor dependencies with Objects.requireNonNull
- Use SLF4J logging
- Log public mutating operations at debug level
- Validate all method inputs with Objects.requireNonNull
  Behavior:

#create
Require non-null command and actor
If a content type with the same key already exists, throw a semantic exception
Create a new ContentType
Initial status should be DRAFT
Save and return it

# activate
Require non-null command and actor
Load the content type by key
If missing, throw NotFoundException
If already ACTIVE, return the existing content type without saving
If ARCHIVED, throw an invalid transition exception
If DRAFT, save a copy with status ACTIVE

# archive
Require non-null command and actor
Load the content type by key
If missing, throw NotFoundException
If already ARCHIVED, return the existing content type without saving
If DRAFT or ACTIVE, save a copy with status ARCHIVED

# findByKey
Require non-null key and actor
Delegate to repository

# findAll
Require non-null actor
Delegate to repository

Exceptions

Create semantic exceptions only if they do not already exist.

Suggested exceptions:
```
ContentTypeAlreadyExistsException
InvalidContentTypeStatusTransitionException
```

Location: codex.codex.internal.service

Do not create a large exception hierarchy unless the project already has one.

Use existing NotFoundException from fundamentum if available.

# Events

Do not implement content type events in this task.

However, design the service so that it can later be decorated by an event-publishing wrapper, following the same pattern used for SiteService.

Possible future events:

```
ContentTypeCreatedEvent
ContentTypeActivatedEvent
ContentTypeArchivedEvent
```

# Runtime

Do not modify CodexRuntime in this task unless all ContentType pieces compile and adding the service is straightforward.

If modifying CodexRuntime, expose:

```
public ContentTypeService contentTypeService()
```

and wire:

```
MemoryContentTypeRepository
CodexContentTypeService
```

Do not add event publishing for content types yet.

# Tests

Add plain JUnit 5 tests.

No Spring.

No mocks unless absolutely necessary.

Prefer real in-memory repository.

## Repository tests

Create tests for MemoryContentTypeRepository:

save should return saved content type
findByKey should return saved content type
findByKey should return empty when missing
existsByKey should return true when saved
existsByKey should return false when missing
findAll should return all saved content types
required arguments should not accept null
Service tests

## Create tests for CodexContentTypeService:

create should create draft content type
create should throw when key already exists
findByKey should return existing content type
findByKey should return empty when missing
findAll should return all content types
activate should move draft to active
activate should be idempotent when already active
activate should fail when archived
archive should move draft to archived
archive should move active to archived
archive should be idempotent when already archived
missing content type should throw NotFoundException for activate
missing content type should throw NotFoundException for archive
required arguments should not accept null

## Constraints
Follow CLAUDE.md conventions.
Follow the same style used by the existing Site implementation.
No Spring annotations.
No JPA.
No persistence framework.
No REST controllers.
No JavaScript extension system.
No permission service yet.
No content item implementation yet.
No event wrapper yet.
No broad refactors.
Do not modify unrelated files.
Keep comments and JavaDoc in English.
Prefer small, explicit, testable classes.
Run the smallest relevant Maven test or compile command after implementation.