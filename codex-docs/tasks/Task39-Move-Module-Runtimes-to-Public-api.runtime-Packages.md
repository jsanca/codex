````markdown id="task39-move-module-runtimes-to-api-runtime"
# Task39: Move Module Runtimes to Public api.runtime Packages

## Objective

Move module runtime classes out of `internal` packages and into public `api.runtime` packages.

This removes the qualified exports currently used by `codex-concilium` to access internal runtime classes.

Current controlled debt:

```java
exports codex.codex.internal.runtime to codex.concilium;
exports codex.index.internal to codex.concilium;
exports codex.chronicon.internal to codex.concilium;
````

After this task, `codex-concilium` should depend on public runtime APIs instead of internal packages.

## Decision Context

`ConciliumRuntime` now composes:

* `CodexRuntime`
* `IndexRuntime`
* `ChroniconRuntime`

That means those runtime classes are no longer internal implementation details. They are the public composition entry points for their modules.

The rule is:

```text
internal
  subscribers
  mappers
  memory repositories
  helpers
  implementation details

api.runtime
  module runtimes intended for Concilium, Porta, future assembly, or manual composition
```

This task should only move runtime classes and update module exports/imports.

Do not redesign the runtimes.

Do not introduce ServiceLoader.

Do not introduce Spring.

Do not change the event pipeline behavior.

## Scope

Move:

```text
codex.codex.internal.runtime.CodexRuntime
  -> codex.codex.api.runtime.CodexRuntime

codex.index.internal.IndexRuntime
  -> codex.index.api.runtime.IndexRuntime

codex.chronicon.internal.ChroniconRuntime
  -> codex.chronicon.api.runtime.ChroniconRuntime
```

Update:

* package declarations
* imports
* tests
* module-info exports
* qualified exports
* README/docs if useful

Do not move:

* subscribers
* mappers
* memory repositories
* projection sources
* event classes
* services
* canonical repositories
* internal helpers

## 1. Move CodexRuntime

Move:

```text
codex-codex/src/main/java/codex/codex/internal/runtime/CodexRuntime.java
```

to:

```text
codex-codex/src/main/java/codex/codex/api/runtime/CodexRuntime.java
```

Update package declaration:

```java
package codex.codex.api.runtime;
```

Requirements:

* preserve public API and behavior
* preserve `inMemory()`
* preserve `inMemory(CodexEventDispatcher)`
* preserve `contentItemProjectionReader()`
* preserve event recorder behavior
* preserve `AutoCloseable` / close behavior if present
* do not change domain/runtime behavior
* update all imports and tests

If `CodexRuntime` still needs internal services/repositories, it may import them from inside the same module. That is allowed.

## 2. Move IndexRuntime

Move:

```text
codex-index/src/main/java/codex/index/internal/IndexRuntime.java
```

to:

```text
codex-index/src/main/java/codex/index/api/runtime/IndexRuntime.java
```

Update package declaration:

```java
package codex.index.api.runtime;
```

Requirements:

* preserve public API and behavior
* preserve `inMemory(ContentItemProjectionReader)`
* preserve `withWriter(ContentItemProjectionReader, IndexWriter)`
* preserve `indexWriter()`
* preserve subscribers list
* preserve close behavior
* keep actual subscribers/mappers/projection sources internal
* update tests and imports

`IndexRuntime` may import internal index classes because it lives in the same module. That is allowed.

## 3. Move ChroniconRuntime

Move:

```text
codex-chronicon/src/main/java/codex/chronicon/internal/ChroniconRuntime.java
```

to:

```text
codex-chronicon/src/main/java/codex/chronicon/api/runtime/ChroniconRuntime.java
```

Update package declaration:

```java
package codex.chronicon.api.runtime;
```

Requirements:

* preserve public API and behavior
* preserve `inMemory()`
* preserve `withRepository(ChroniconRepository)`
* preserve `repository()`
* preserve subscribers list
* preserve close behavior
* keep actual subscribers/repositories internal
* update tests and imports

`ChroniconRuntime` may import internal chronicon classes because it lives in the same module. That is allowed.

## 4. Update module-info.java

### codex-codex

Add:

```java
exports codex.codex.api.runtime;
```

Remove:

```java
exports codex.codex.internal.runtime to codex.concilium;
```

if present.

Do not export other internal packages.

### codex-index

Add:

```java
exports codex.index.api.runtime;
```

Remove:

```java
exports codex.index.internal to codex.concilium;
```

if present.

Do not export `codex.index.internal`.

### codex-chronicon

Add:

```java
exports codex.chronicon.api.runtime;
```

Remove:

```java
exports codex.chronicon.internal to codex.concilium;
```

if present.

Do not export `codex.chronicon.internal`.

### codex-concilium

Update imports to use:

```java
codex.codex.api.runtime.CodexRuntime
codex.index.api.runtime.IndexRuntime
codex.chronicon.api.runtime.ChroniconRuntime
```

No qualified access to internal runtime packages should remain.

## 5. Update Tests

Move or update runtime tests as needed.

Likely package updates:

```text
codex-codex runtime tests
codex-index IndexRuntimeTest
codex-chronicon ChroniconRuntimeTest
codex-concilium ConciliumRuntimeTest
```

Tests may remain in internal test packages if useful, but imports should use the new public runtime packages.

Do not weaken tests.

Do not delete runtime behavior tests.

## 6. Search and Cleanup

After moving, search for old imports/references:

```text
codex.codex.internal.runtime.CodexRuntime
codex.index.internal.IndexRuntime
codex.chronicon.internal.ChroniconRuntime
exports codex.codex.internal.runtime to codex.concilium
exports codex.index.internal to codex.concilium
exports codex.chronicon.internal to codex.concilium
```

They should be gone.

It is okay for runtime classes to import internal implementation classes from their own module.

It is not okay for `codex-concilium` to import internal runtime packages.

## 7. Documentation

Update READMEs or module responsibility docs lightly if needed.

Suggested note:

```text
Module runtimes are now public composition APIs under *.api.runtime packages.
Internal subscribers, mappers, repositories, and helpers remain internal.
```

Do not rewrite large docs.

## 8. Acceptance Criteria

Task is complete when:

* `CodexRuntime` lives in `codex.codex.api.runtime`
* `IndexRuntime` lives in `codex.index.api.runtime`
* `ChroniconRuntime` lives in `codex.chronicon.api.runtime`
* public runtime packages are exported
* internal runtime qualified exports to `codex-concilium` are removed
* `codex-concilium` imports only public runtime packages
* no runtime behavior changes
* tests pass
* full reactor compile passes

## 9. Maven Commands

Run:

```bash
mvn test -pl codex-codex,codex-index,codex-chronicon,codex-concilium -am
```

Then run:

```bash
mvn compile
```

Report command results.

## 10. Post-Task Report

After implementation, report:

* files moved
* files modified
* module-info changes
* imports updated
* old internal references removed
* Maven commands run
* whether tests passed
* intentional deviations
* open questions
* recommended follow-up tasks

## 11. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Move runtime classes only.
* Do not move subscribers.
* Do not move mappers.
* Do not move memory repositories.
* Do not move services.
* Do not move canonical repositories.
* Do not change event pipeline behavior.
* Do not introduce ServiceLoader.
* Do not introduce Spring.
* Do not introduce Service Locator.
* Do not create new runtime abstractions.
* Do not wire Porta.
* Do not add REST/GraphQL.
* Do not add persistence.
* Do not add cache/search/workflow changes.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.
* Prefer small, mechanical package moves.

