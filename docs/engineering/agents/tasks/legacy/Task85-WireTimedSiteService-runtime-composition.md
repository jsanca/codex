# Task 85 — Wire TimedSiteService into Codex Runtime Composition

## Goal

Integrate `TimedSiteService` into the Codex service composition path so `SiteService` operations are observed when an `Observance` instance is provided.

This task turns `TimedSiteService` from an isolated decorator into part of the runtime pipeline.

---

## Scope

Identify the runtime/factory/composition point where `SiteService` is created or exposed.

Wrap the effective `SiteService` with `TimedSiteService` when composing the runtime.

The expected conceptual pipeline is:

```text
TimedSiteService
  -> existing SiteService pipeline
```

or, if other wrappers already exist:

```text
TimedSiteService
  -> Transactional/Locking/Auditing/Future wrappers
      -> CoreSiteService
```

The exact placement should preserve current behavior.

---

## Requirements

## 1. Runtime integration

Update the appropriate runtime/factory class so that the exposed `SiteService` is timed.

Use constructor-injected `Observance`.

If existing factory methods do not accept Observance yet, add an Observance-aware factory while preserving the existing no-op path.

Example direction:

```java
CodexRuntime.inMemory()
CodexRuntime.inMemory(Observance observance)
```

or whatever naming matches the current runtime style.

---

## 2. Preserve no-op defaults

Existing factory methods should continue to work.

They should delegate to the Observance-aware path using:

```java
Observance.noop()
```

---

## 3. Preserve existing behavior

The integration must not change:

* service semantics
* return values
* exception behavior
* event dispatch behavior
* transaction behavior
* locking behavior

Observance remains observational only.

---

## 4. Add integration tests

Add tests proving that:

* runtime-created `SiteService` records duration metrics
* failures are observed through the runtime path
* existing no-op runtime construction still works
* the exposed service remains behaviorally equivalent

Avoid vendor-specific assertions.

Use existing recording/in-memory observance test utilities.

---

## Constraints

Do not introduce:

* Micrometer
* Prometheus
* OpenTelemetry
* AOP
* proxies
* reflection
* annotations
* external dependencies

Keep the implementation aligned with ADR-008.

---

## Notes

`TimedSiteService` measures caller-visible domain operation cost.

This is different from:

* dispatcher metrics
* index writer metrics
* future cache metrics
* future persistence metrics

The goal is to establish runtime composition as the standard place where cross-cutting service decorators are assembled.
