# Task8: System Site and Lifecycle Participation

## Objective

Introduce a first-class lifecycle participation model for Codex resources and apply it to `Site`.

This task adds support for a built-in system site that is addressable through `SiteKey.SYSTEM`, but does not participate in the normal user-managed site lifecycle.

The goal is to avoid hardcoding checks such as `site.key().equals(SiteKey.SYSTEM)` inside services. Services should validate lifecycle participation semantically instead.

## Architectural direction

Do not ask:

`is this the system site?`

Ask:

`does this resource participate in the normal lifecycle?`

This prepares Codex for future resource types such as:

- system resources
- read-only resources
- externally managed resources
- virtual resources
- treeable resources
- resources backed by S3, filesystem, database, or future providers

This task should implement only the lifecycle participation foundation for `Site`.

Do not implement `TreeableResource` or `CodexResourcePath` in this task.

## Scope

Implement:

- `SiteKey.SYSTEM`
- `LifecycleParticipation`
- `Site.lifecycleParticipation`
- `Site.system()`
- validation pipeline inside `CodexSiteService`
- semantic exception for invalid lifecycle operations
- tests

Do not implement:

- TreeableResource
- CodexResourcePath
- resource protocols
- resource metadata
- ContentType scoping
- ContentType ownership
- permission service
- Spring integration
- persistence
- JavaScript extensions

## Location

Use existing packages where possible.

Suggested locations:

- `codex.codex.api.model.identity.SiteKey`
- `codex.fundamentum.api.lifecycle.LifecycleParticipation`
- `codex.codex.api.model.entity.Site`
- `codex.codex.internal.service.CodexSiteService`
- `codex.codex.internal.service.InvalidSiteLifecycleOperationException`

If the project already has a better package for lifecycle concepts in `codex-fundamentum`, use it.

## 1. Add SiteKey.SYSTEM

Update `SiteKey` to include a strongly typed system site key.

Requirements:

- Add `public static final SiteKey SYSTEM`
- Value should be `"system"`
- Do not use raw string constants outside `SiteKey`
- Keep existing validation rules

Expected usage:

`SiteKey.SYSTEM`

Do not introduce `Scope.GLOBAL` or `Scope.SITE` in this task.

Global resources will later use `SiteKey.SYSTEM` as their site scope.

## 2. Add LifecycleParticipation

Create enum:

`codex.fundamentum.api.lifecycle.LifecycleParticipation`

Values:

- `MANAGED`
- `READ_ONLY`
- `SYSTEM_MANAGED`
- `EXTERNAL`

Semantics:

- `MANAGED`: participates in the normal Codex lifecycle
- `READ_ONLY`: visible and addressable, but not mutable through normal services
- `SYSTEM_MANAGED`: managed by Codex runtime/platform, not by normal user lifecycle operations
- `EXTERNAL`: represents a resource whose lifecycle is owned outside Codex

Add JavaDoc to the enum and each value.

Do not add behavior to the enum yet.

## 3. Update Site

Update `Site` to include lifecycle participation.

Add field:

`LifecycleParticipation lifecycleParticipation`

Requirements:

- Import `codex.fundamentum.api.lifecycle.LifecycleParticipation`
- Add field to the `Site` record
- Default to `LifecycleParticipation.MANAGED` when null
- Add builder support
- Add copyOf support if `Site.copyOf(...)` exists
- Include in `toString()`
- Existing regular sites should continue to work without explicitly setting lifecycle participation

## 4. Add Site.system()

Add a static factory method to `Site`:

`public static Site system()`

Requirements:

- Returns a new `Site` instance representing the built-in system site
- Uses `SiteKey.SYSTEM`
- Uses a stable system id
- Display name should be `"System"`
- Status should be `SiteStatus.STARTED`
- Lifecycle participation should be `LifecycleParticipation.SYSTEM_MANAGED`
- Should not require persistence
- Should not rely on external infrastructure

If `SiteId` does not yet have a system id factory, add one:

`SiteId.system()`

The system id value should be stable and deterministic.

Suggested value:

`system`

Do not create a mutable static `Site SYSTEM` instance. Use a factory method instead.

## 5. Add lifecycle operation exception

Create:

`codex.codex.internal.service.InvalidSiteLifecycleOperationException`

Requirements:

- Runtime exception
- Semantic exception used when a site does not participate in the requested lifecycle operation
- Include the `Site` when useful, similar to existing internal service exceptions
- Keep it simple
- Do not create a large exception hierarchy unless already present

Example message style:

`Site system does not participate in the normal lifecycle operation: SUSPEND`

## 6. Add validation pipeline in CodexSiteService

Refactor `CodexSiteService` so lifecycle-changing operations use a validation pipeline.

Do not scatter direct checks across public methods.

Add a private validation method pattern such as:

`runValidation(...)`

The method should be responsible for generic service validations that apply to lifecycle operations.

Suggested structure:

- public method validates command and actor with `Objects.requireNonNull`
- public method loads or delegates to internal lifecycle method
- internal lifecycle method loads the current site
- idempotent transitions return the existing site before saving
- before changing state, call `runValidation(...)`
- `runValidation(...)` should validate lifecycle participation and status transition rules

Add an internal operation enum if useful:

`SiteOperation`

Suggested values:

- `START`
- `SUSPEND`
- `ARCHIVE`
- `UNARCHIVE`

This enum can remain private inside `CodexSiteService`.

## 7. Lifecycle participation validation

Inside the validation pipeline, reject lifecycle operations on sites that are not `LifecycleParticipation.MANAGED`.

Rules:

- `MANAGED`: lifecycle operations are allowed
- `READ_ONLY`: lifecycle operations are rejected
- `SYSTEM_MANAGED`: lifecycle operations are rejected
- `EXTERNAL`: lifecycle operations are rejected

Rejected operations should throw `InvalidSiteLifecycleOperationException`.

Do not check specifically for `SiteKey.SYSTEM`.

The validation should be based on `LifecycleParticipation`.

## 8. Preserve existing status transition rules

Keep the existing site status transition rules:

- `STARTED -> SUSPENDED` is valid
- `STARTED -> STARTED` is idempotent
- `STARTED -> ARCHIVED` is invalid
- `SUSPENDED -> STARTED` is valid
- `SUSPENDED -> ARCHIVED` is valid
- `SUSPENDED -> SUSPENDED` is idempotent
- `ARCHIVED -> SUSPENDED` is valid through `unarchive`
- `ARCHIVED -> ARCHIVED` is idempotent
- `ARCHIVED -> STARTED` is invalid

Invalid status transitions should continue to throw `InvalidSiteStatusTransitionException`.

Lifecycle participation validation and status transition validation are separate concerns.

## 9. Idempotency behavior

Idempotent operations should continue to return the existing site and avoid saving.

Examples:

- `start` on an already `STARTED` site returns the current site
- `suspend` on an already `SUSPENDED` site returns the current site
- `archive` on an already `ARCHIVED` site returns the current site

Important:

Even if a site is `SYSTEM_MANAGED`, an idempotent operation should still be considered carefully.

For this task, lifecycle participation should be validated before allowing any lifecycle operation on a non-managed site, even if the target status equals the current status.

This means calling `start` on `Site.system()` should throw `InvalidSiteLifecycleOperationException`, not silently return the system site.

## 10. Tests

Add or update plain JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real in-memory repository.

### SiteKey tests

Add tests for:

- `SiteKey.SYSTEM` has value `"system"`
- `SiteKey.SYSTEM` equals `SiteKey.of("system")`

### Site tests

Add tests for:

- regular site defaults lifecycle participation to `MANAGED`
- builder can set lifecycle participation
- `Site.copyOf(...)` preserves lifecycle participation
- `Site.system()` returns a site with key `SiteKey.SYSTEM`
- `Site.system()` returns a site with status `STARTED`
- `Site.system()` returns lifecycle participation `SYSTEM_MANAGED`

### CodexSiteService tests

Add tests for:

- `start` rejects `SYSTEM_MANAGED` site
- `suspend` rejects `SYSTEM_MANAGED` site
- `archive` rejects `SYSTEM_MANAGED` site
- `unarchive` rejects `SYSTEM_MANAGED` site
- lifecycle rejection throws `InvalidSiteLifecycleOperationException`
- rejection is based on lifecycle participation, not hardcoded key
- existing status transition tests still pass

Add at least one test showing a non-system site with `LifecycleParticipation.SYSTEM_MANAGED` is also rejected. This proves the service is not checking only `SiteKey.SYSTEM`.

## 11. Runtime

Do not wire `Site.system()` into `CodexRuntime` in this task.

Do not preload the system site into `MemorySiteRepository` in this task.

This task only introduces the model and validation semantics.

Runtime/system-site lookup behavior will be handled later, possibly through:

- runtime preloading
- repository decorator
- system resource provider
- resource resolver

## 12. Documentation

Add JavaDoc where appropriate.

Add a short note to the relevant ADR or create a small draft note if there is no appropriate ADR yet.

The note should explain:

- global resources will use `SiteKey.SYSTEM`
- `Site.system()` represents a platform-managed site
- the system site does not participate in the normal lifecycle
- lifecycle validation is based on `LifecycleParticipation`, not hardcoded keys
- treeable/resource-path support is future work

## Constraints

- Follow CLAUDE.md conventions.
- Follow the style of the existing Site implementation.
- No Spring annotations.
- No JPA.
- No persistence framework.
- No REST controllers.
- No JavaScript extension system.
- No permission service.
- No ContentType changes in this task.
- No TreeableResource implementation in this task.
- No CodexResourcePath implementation in this task.
- No broad refactors.
- Do not modify unrelated files.
- Do not modify `.idea`, `target`, `build`, or generated files.
- Keep comments and JavaDoc in English.
- Prefer small, explicit, testable changes.
- Run the smallest relevant Maven test or compile command after implementation.