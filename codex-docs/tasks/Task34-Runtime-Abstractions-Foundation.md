````markdown id="task34-runtime-abstractions-foundation"
# Task34: Runtime Abstractions Foundation

## Objective

Introduce the first generic runtime composition abstractions in `codex-fundamentum`.

These abstractions will support future module runtimes such as:

- `CodexRuntime` / future `CoreRuntime`
- `IndexRuntime`
- `ChroniconRuntime`
- `ArchivumRuntime`
- `PortaRuntime`
- future application/runtime assembly modules

This task defines the shared runtime vocabulary only.

Do not create `ChroniconRuntime` yet.

Do not create `IndexRuntime` yet.

Do not create `codex-runtime` or `codex-assembly`.

Do not implement ServiceLoader discovery yet.

## Decision Context

Codex is evolving into a modular system.

Each module may eventually know how to construct its own internal objects:

```text
codex-codex
  -> canonical services, repositories, core event pipeline

codex-index
  -> index writers, mappers, indexing subscribers

codex-chronicon
  -> audit repositories, audit subscribers

codex-porta
  -> REST/GraphQL/WebSocket/SSE adapters
````

A future leaf module or assembly layer may compose these module runtimes:

```text
CoreRuntime
  + IndexRuntime
  + ChroniconRuntime
  + PortaRuntime
  -> ApplicationRuntime
```

The important rule:

```text
ServiceLoader may discover module runtime providers later.
RuntimeContext may help assemble modules later.
But domain objects must never use ServiceLoader or a Service Locator.
```

Runtime composition belongs at the edge.

Domain services, entities, repositories, subscribers, and canonical logic must receive dependencies explicitly.

## Scope

Implement in `codex-fundamentum`:

* `CodexModuleRuntime`
* `CodexModuleRuntimeProvider`
* `CodexRuntimeContext`
* a simple map-backed context implementation if useful
* tests
* documentation update if useful

Do not implement:

* `ChroniconRuntime`
* `IndexRuntime`
* `CoreRuntime` rename
* `codex-runtime`
* `codex-assembly`
* ServiceLoader provider declarations
* dynamic registry
* OSGi-style service registry
* Spring integration
* runtime auto-discovery
* runtime lifecycle manager
* dependency injection container
* Service Locator in domain code

## Package Location

Use:

```text
codex.fundamentum.api.runtime
```

Suggested files:

```text
codex.fundamentum.api.runtime.CodexModuleRuntime
codex.fundamentum.api.runtime.CodexModuleRuntimeProvider
codex.fundamentum.api.runtime.CodexRuntimeContext
codex.fundamentum.api.runtime.MapBackedCodexRuntimeContext
```

If a different naming convention already exists in `fundamentum`, follow it.

Update `module-info.java` to export:

```java
exports codex.fundamentum.api.runtime;
```

## 1. Create CodexModuleRuntime

Create:

```text
codex.fundamentum.api.runtime.CodexModuleRuntime
```

Suggested shape:

```java
public interface CodexModuleRuntime extends AutoCloseable {

    String moduleName();

    List<CodexEventSubscriber<? extends CodexEvent>> subscribers();

    @Override
    default void close() {
    }
}
```

Requirements:

* extends `AutoCloseable`
* `moduleName()` identifies the module/runtime
* `subscribers()` returns event subscribers contributed by this module
* default `subscribers()` may return `List.of()` if preferred
* default `close()` should be no-op
* JavaDoc must explain this is a module composition contract, not a domain service
* no dependency lookup methods
* no service locator methods
* no start method yet unless strongly justified

Important:

`CodexModuleRuntime` may expose subscribers because event-driven projections are a common module integration point.

Do not add arbitrary `get(Class<T>)` methods here.

## 2. Create CodexModuleRuntimeProvider

Create:

```text
codex.fundamentum.api.runtime.CodexModuleRuntimeProvider
```

Suggested shape:

```java
public interface CodexModuleRuntimeProvider {

    String moduleName();

    CodexModuleRuntime create(CodexRuntimeContext context);
}
```

Requirements:

* JavaDoc must explain this is intended for future `ServiceLoader` discovery
* no implementation yet
* no `provides` declarations in module-info yet
* no ServiceLoader usage in this task
* `create(...)` must accept a runtime composition context
* implementations should be module-specific in future tasks

This interface is for discovering module runtime providers later, not for resolving arbitrary dependencies in the domain.

## 3. Create CodexRuntimeContext

Create:

```text
codex.fundamentum.api.runtime.CodexRuntimeContext
```

Suggested shape:

```java
public interface CodexRuntimeContext {

    <T> Optional<T> find(Class<T> type);

    <T> T require(Class<T> type);
}
```

Requirements:

* this context is only for runtime/module assembly
* JavaDoc must strongly warn that it must not be passed into domain services/entities
* `find(...)` returns optional
* `require(...)` returns the object or throws if missing
* no mutation methods on the interface unless needed
* no global singleton
* no static global registry
* no ServiceLocator terminology

Exception behavior:

* null type should be rejected
* missing required type should throw an unchecked exception
* `IllegalStateException` is acceptable for missing required runtime dependency
* do not introduce checked exceptions

## 4. Create MapBackedCodexRuntimeContext

Create:

```text
codex.fundamentum.api.runtime.MapBackedCodexRuntimeContext
```

Purpose:

* simple immutable implementation for tests/manual composition
* not a global registry
* not a dynamic service registry

Suggested shape:

```java
public final class MapBackedCodexRuntimeContext implements CodexRuntimeContext {

    public static Builder builder()

    public static MapBackedCodexRuntimeContext empty()

    public <T> Optional<T> find(Class<T> type)

    public <T> T require(Class<T> type)
}
```

Builder:

```java
public static final class Builder {
    public <T> Builder put(Class<T> type, T instance)
    public MapBackedCodexRuntimeContext build()
}
```

Requirements:

* validate non-null type
* validate non-null instance
* store by `Class<?>`
* defensively copy map on build
* no mutation after build
* `find(...)` should type-check/cast safely using `Class.cast(...)`
* `require(...)` should throw `IllegalStateException` with useful message if missing
* do not allow multiple values for the same type yet
* do not support dynamic registration/removal
* do not add lifecycle behavior here

Important:

This class exists to support explicit assembly and tests.

It is not a Service Locator for domain logic.

## 5. Documentation Guidance

Add JavaDoc and/or a short documentation note explaining:

```text
Allowed:
- use CodexRuntimeContext while constructing module runtimes
- use ServiceLoader later to discover CodexModuleRuntimeProvider
- pass explicit dependencies into constructors after resolving them at the composition edge

Not allowed:
- pass CodexRuntimeContext into domain services
- call context.require(...) inside entities
- call context.require(...) inside canonical lifecycle services
- use a static/global runtime context
- use this as a hidden dependency lookup mechanism
```

If there is an existing architecture document for module runtime composition, update it lightly.

Do not create a large ADR unless the project convention requires it.

## 6. Tests

Add JUnit 5 tests in `codex-fundamentum`.

No Spring.

No Mockito.

### CodexModuleRuntime tests

If methods have defaults, test:

* default subscribers returns empty list
* default close does not throw
* simple anonymous/test runtime can provide moduleName

If no default subscribers is used, test via a small test implementation.

### CodexModuleRuntimeProvider tests

Simple compile/behavior test:

* provider returns module name
* provider creates runtime from context

Use a local test implementation.

### CodexRuntimeContext / MapBackedCodexRuntimeContext tests

Add tests for:

* empty context find returns empty
* empty context require throws `IllegalStateException`
* builder put stores instance
* find returns instance by type
* require returns instance by type
* find rejects null type
* require rejects null type
* builder put rejects null type
* builder put rejects null instance
* build defensively copies entries
* later builder changes do not mutate built context if applicable
* wrong type is not returned
* duplicate put for same type overwrites previous value or rejects; choose one and document it

Recommendation:

Reject duplicate type registrations if simple.

Reason:

For module assembly, duplicate services should be explicit, not silently overwritten.

If duplicate rejection is implemented, add a test.

## 7. Module Info

Update `codex-fundamentum/src/main/java/module-info.java`.

Add export:

```java
exports codex.fundamentum.api.runtime;
```

No other module should need changes in this task unless tests require them.

## 8. Acceptance Criteria

Task is complete when:

* `CodexModuleRuntime` exists
* `CodexModuleRuntimeProvider` exists
* `CodexRuntimeContext` exists
* map-backed context implementation exists
* `codex-fundamentum` exports runtime package
* tests pass
* no ServiceLoader usage is implemented
* no global registry is implemented
* no domain module uses these abstractions yet
* no runtime wiring changes are made outside fundamentum

## 9. Maven Commands

Run:

```bash
mvn test -pl codex-fundamentum
```

If practical, also run:

```bash
mvn test -pl codex-fundamentum -am
```

Report command results.

## 10. Post-Task Report

After implementation, report:

* files created
* files modified
* module-info changes
* tests added
* Maven commands run
* whether tests passed
* intentional deviations
* open questions
* recommended follow-up tasks

## 11. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Keep runtime abstractions generic.
* Fundamentum must not become a god module.
* Do not include CMS concepts in fundamentum runtime abstractions.
* Do not mention Site, ContentItem, ContentType, IndexDocument, AuditRecord, Workflow, User, REST, AI, or persistence in API types.
* Do not implement ServiceLoader discovery yet.
* Do not implement dynamic registry.
* Do not implement OSGi-like behavior.
* Do not introduce Spring.
* Do not introduce dependency injection container.
* Do not introduce Service Locator in domain code.
* Do not change CodexRuntime.
* Do not create ChroniconRuntime.
* Do not create IndexRuntime.
* Do not modify codex-codex, codex-index, or codex-chronicon unless strictly necessary for compilation.
* Keep comments and JavaDoc in English.
* Prefer small, explicit, testable abstractions.

```
```
