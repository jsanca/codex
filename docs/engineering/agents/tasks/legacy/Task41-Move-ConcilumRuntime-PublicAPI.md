# Task41: Move ConciliumRuntime to Public api.runtime Package

## Objective

Move `ConciliumRuntime` out of the internal package and into a public `api.runtime` package.

Current state:

```text
codex.codex.api.runtime.CodexRuntime
codex.index.api.runtime.IndexRuntime
codex.chronicon.api.runtime.ChroniconRuntime
codex.concilium.internal.ConciliumRuntime
````

`ConciliumRuntime` is now a real composition entry point and should follow the same public runtime convention.

Target state:

```text
codex.concilium.api.runtime.ConciliumRuntime
```

This is a small technical debt cleanup task.

Do not change runtime behavior.

Do not introduce ServiceLoader.

Do not introduce Spring.

Do not introduce Porta integration.

## Decision Context

Task39 moved module runtimes to public `*.api.runtime` packages.

Task40 proved that `ConciliumRuntime` composes core, index, and chronicon successfully through an end-to-end publish projection smoke test.

Since `ConciliumRuntime` is intended to be consumed later by edge modules such as `codex-porta`, CLI, workers, or embedded launchers, it should not remain in an internal package.

Rule:

```text
internal
  subscribers
  mappers
  memory repositories
  helpers
  implementation details

api.runtime
  module/application runtimes intended for composition layers
```

Concilium follows the same rule.

## Scope

Move:

```text
codex-concilium/src/main/java/codex/concilium/internal/ConciliumRuntime.java
```

to:

```text
codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java
```

Update:

* package declaration
* imports
* tests
* README/docs if needed
* module-info exports

Do not implement:

* ServiceLoader provider
* `ConciliumRuntimeProvider`
* Spring configuration
* Porta integration
* REST/GraphQL
* CLI
* global runtime registry
* dynamic runtime discovery
* cache/search/persistence/workflow changes

## 1. Move ConciliumRuntime

Update package declaration:

```java
package codex.concilium.api.runtime;
```

Preserve all existing behavior:

* `inMemory()`
* `compose(...)`
* child runtime accessors
* `eventDispatcher()`
* `subscribers()`
* `moduleName()`
* `close()`
* event pipeline behavior
* AtomicReference forwarding lambda if currently used

Do not refactor the runtime design in this task.

## 2. Update module-info.java

Update:

```text
codex-concilium/src/main/java/module-info.java
```

Add:

```java
exports codex.concilium.api.runtime;
```

Do not export:

```java
codex.concilium.internal
```

unless it is still required for tests or existing design. Prefer not to export internals.

Do not add `provides` declarations.

Do not add new module dependencies unless strictly required.

## 3. Update Tests

Update all tests importing:

```text
codex.concilium.internal.ConciliumRuntime
```

to:

```text
codex.concilium.api.runtime.ConciliumRuntime
```

Likely tests:

* `ConciliumRuntimeTest`
* `ConciliumRuntimeEndToEndTest`

Tests may remain physically under `src/test/java/codex/concilium/internal` if that is the current convention, but they should import the public runtime class.

Do not delete or weaken tests.

## 4. Update Documentation

Update `codex-concilium/README.md` lightly if it references the old package.

Suggested note:

```text
ConciliumRuntime is exposed as the public composition runtime under codex.concilium.api.runtime.
```

Update `MODULE-RESPONSIBILITIES.md` only if it contains package-level references. Do not rewrite large docs.

## 5. Search and Cleanup

Search for old references:

```text
codex.concilium.internal.ConciliumRuntime
```

They should be gone from production and test imports.

It is okay for old text to appear in git history, but not in active Java source.

## 6. Acceptance Criteria

Task is complete when:

* `ConciliumRuntime` lives in `codex.concilium.api.runtime`
* `codex-concilium` exports `codex.concilium.api.runtime`
* `codex.concilium.internal` is not exported
* all tests import the public runtime package
* runtime behavior is unchanged
* `ConciliumRuntimeEndToEndTest` still passes
* no ServiceLoader/Spring/Porta integration is added
* full compile passes

## 7. Maven Commands

Run:

```bash
mvn test -pl codex-concilium -am
```

Then run:

```bash
mvn compile
```

Report command results.

## 8. Post-Task Report

After implementation, report:

* files moved
* files modified
* module-info changes
* imports updated
* old references removed
* Maven commands run
* whether tests passed
* intentional deviations
* open questions
* recommended follow-up tasks

## 9. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Move `ConciliumRuntime` only.
* Do not change runtime behavior.
* Do not redesign event dispatching.
* Do not introduce ServiceLoader.
* Do not introduce Spring.
* Do not introduce Service Locator.
* Do not wire Porta.
* Do not add REST/GraphQL.
* Do not add CLI.
* Do not add persistence.
* Do not add cache/search/workflow behavior.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.
* Prefer a small mechanical package move.

