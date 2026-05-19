# Task 87 — Add TimedContentItemService Observance Decorator

## Goal

Replicate the timed service Observance pattern for `ContentItemService`.

`TimedContentItemService` should measure caller-visible operation duration and operation failures for content item operations.

This extends Observance from site management and content modeling into the actual content instance layer.

---

## Context

The following timed service decorators already exist and are wired into `CodexRuntime`:

```text
TimedSiteService
  -> EventPublishingSiteService
      -> CodexSiteService
```

```text
TimedContentTypeService
  -> EventPublishingContentTypeService
      -> CodexContentTypeService
```

The intended service instrumentation model is:

```text
TimedService
  -> current effective service pipeline
      -> core domain service
```

The timed decorator should remain the outermost layer for now, measuring full caller-visible cost.

This includes, now or in the future:

* event publishing
* transaction handling
* locking
* auditing
* security checks
* custom hooks
* core business logic

---

## Scope

Create a `TimedContentItemService` decorator for `ContentItemService`.

Wire it into the runtime composition path where `ContentItemService` is created or exposed.

Use `TimedSiteService` and `TimedContentTypeService` as reference implementations.

---

## Requirements

## 1. Create `ContentItemServiceMetricNames`

Add package-private metric name constants for all observed `ContentItemService` operations.

Follow the naming style already used by:

```text
SiteServiceMetricNames
ContentTypeServiceMetricNames
```

Metric names should be:

* stable
* low-cardinality
* operation-specific
* implementation-agnostic

Avoid dynamic values in metric names.

Do not include:

* site keys
* content type keys
* content item keys
* revision ids
* actor ids
* field names
* arbitrary user input

---

## 2. Implement `TimedContentItemService`

Create a final decorator class that:

* implements `ContentItemService` directly or uses an existing forwarding interface if available
* receives `ContentItemService delegate`
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

Update the relevant runtime/factory/composition code so the exposed `ContentItemService` is timed.

The timed decorator should be the outermost layer for now.

Conceptually:

```text
TimedContentItemService
  -> current effective ContentItemService pipeline
```

If an event-publishing content item service already exists, the expected chain should be:

```text
TimedContentItemService
  -> EventPublishingContentItemService
      -> CodexContentItemService
```

If no event-publishing decorator exists yet, wrap the current effective implementation without introducing new event behavior.

Do not alter current service semantics.

Preserve existing no-op behavior for runtime factories that do not receive an explicit `Observance`.

---

## 4. Add tests

Add focused tests for `TimedContentItemService`.

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

* runtime-created `ContentItemService` records duration metrics
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

This task should mostly mirror `TimedSiteService` and `TimedContentTypeService`.

Do not redesign Observance.

Do not introduce a shared timed-service abstraction yet.

Duplication is acceptable at this stage because the pattern is still being validated across service boundaries.

The goal is consistency and full service coverage, not premature abstraction.
    