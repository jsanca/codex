# Task 86 — Add TimedContentTypeService Observance Decorator

## Goal

Replicate the `TimedSiteService` pattern for `ContentTypeService`.

`TimedContentTypeService` should measure caller-visible operation duration and operation failures for content type operations.

This extends the Observance service-decorator baseline from `SiteService` into the content modeling layer.

---

## Context

`TimedSiteService` is already implemented and wired into `CodexRuntime` as the outermost service decorator.

The intended service instrumentation model is:

```text
TimedService
  -> current effective service pipeline
      -> core domain service
```

The timed decorator should measure the full caller-visible cost of the operation.

This may include, now or in the future:

* event publishing
* transaction handling
* locking
* auditing
* security checks
* custom hooks
* core business logic

---

## Scope

Create a `TimedContentTypeService` decorator for `ContentTypeService`.

Wire it into the runtime composition path where `ContentTypeService` is created or exposed.

Use `TimedSiteService` as the reference implementation.

---

## Requirements

## 1. Create `ContentTypeServiceMetricNames`

Add package-private metric name constants for all observed `ContentTypeService` operations.

Follow the naming style already used by:

```text
SiteServiceMetricNames
```

Metric names should be:

* stable
* low-cardinality
* operation-specific
* implementation-agnostic

Avoid dynamic values in metric names.

---

## 2. Implement `TimedContentTypeService`

Create a final decorator class that:

* implements `ContentTypeService` directly or uses an existing forwarding interface if available
* receives `ContentTypeService delegate`
* receives `Observance observance`
* rejects null constructor arguments
* delegates all service methods
* records duration for each method call
* increments failure counters only when the delegate throws
* does not swallow exceptions
* preserves return values exactly

Duration should be recorded for both successful and failed calls.

---

## 3. Wire into runtime composition

Update the relevant runtime/factory/composition code so the exposed `ContentTypeService` is timed.

The timed decorator should be the outermost layer for now.

Conceptually:

```text
TimedContentTypeService
  -> current effective ContentTypeService pipeline
```

Do not alter current service semantics.

Preserve existing no-op behavior for runtime factories that do not receive an explicit `Observance`.

---

## 4. Add tests

Add focused tests for `TimedContentTypeService`.

Validate:

* constructor rejects null delegate
* constructor rejects null observance
* successful calls record duration
* failed calls record duration
* failed calls increment the failure counter
* successful calls do not increment the failure counter
* delegate is called exactly once
* return values are preserved
* exceptions propagate unchanged
* metrics do not cross-contaminate between operations

Add runtime integration tests if needed to prove:

* runtime-created `ContentTypeService` records duration metrics
* no-op runtime construction still works

---

## Constraints

Do not introduce:

* Micrometer
* Prometheus
* OpenTelemetry
* Spring dependencies
* AOP
* proxies
* reflection
* annotations
* external dependencies

Keep the implementation explicit and aligned with ADR-008.

---

## Notes

This task should mostly mirror `TimedSiteService`.

Do not redesign Observance.

Do not change metric conventions unless there is a clear inconsistency with the existing pattern.

The goal is replication and consistency, not a new abstraction yet.
