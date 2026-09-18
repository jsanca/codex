# Task 84 — Add TimedSiteService Observance Decorator

## Goal

Add caller-visible service-level Observance instrumentation for `SiteService`.

This complements event dispatcher observance:

* dispatcher observance measures the event pipeline
* service observance measures domain operation cost as seen by callers

## Scope

Create a `TimedSiteService` decorator that wraps an existing `SiteService`.

It should measure:

* operation duration
* operation failures

## Requirements

1. Implement `TimedSiteService`

It should delegate all `SiteService` operations to another `SiteService`.

Use the existing forwarding/decorator style if available.

2. Inject dependencies explicitly

Constructor should receive:

```java
SiteService delegate
Observance observance
```

Reject nulls.

3. Measure duration per operation

Use low-cardinality metric names such as:

```text
services.site.create.duration
services.site.start.duration
services.site.suspend.duration
services.site.archive.duration
services.site.find.duration
```

or the existing Codex metric naming convention if already defined.

4. Count failures per operation

Example:

```text
services.site.create.failed
services.site.start.failed
```

Only increment failure counters when the delegate throws.

Do not swallow exceptions.

5. Preserve behavior

The decorator must not change:

* return values
* exception behavior
* idempotency semantics
* event behavior
* transaction behavior

6. Add tests

Use a recording Observance implementation.

Validate:

* successful calls record duration
* failed calls record duration and failure counter
* delegate is called exactly once
* return values are preserved
* exceptions propagate

## Constraints

Do not introduce:

* Micrometer
* Prometheus
* OpenTelemetry
* AOP
* proxies
* reflection
* annotations

Keep instrumentation explicit and implementation-agnostic.

## Notes

This task establishes the reusable pattern for future:

* `TimedContentTypeService`
* `TimedContentItemService`
* `TimedContentRevisionService`
