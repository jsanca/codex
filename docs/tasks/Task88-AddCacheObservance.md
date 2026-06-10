# Task 88 — Add Cache Observance Baseline

## Goal

Add Observance instrumentation to the Codex cache layer.

The goal is to make cache behavior visible without changing cache semantics.

## Scope

Identify the current cache abstractions and decorators used by Codex.

Instrument cache operations with low-cardinality metrics.

Focus on the existing cache regions and operations already present in the codebase.

## Requirements

## 1. Measure cache reads

Record counters for:

* cache hit
* cache miss

Metric names should be stable and low-cardinality.

Example direction:

```text
cache.hit.{region}
cache.miss.{region}
```

or the existing metric naming convention if one already exists.

Do not include dynamic keys in metric names.

## 2. Measure cache writes

Record counters for:

* cache put
* cache remove / invalidate, if supported
* cache clear, if supported

Only instrument operations that already exist.

Do not invent new cache behavior.

## 3. Optional duration metrics

If cache operations already have a natural wrapper point, record duration for:

* get
* put
* invalidate/remove
* clear

If this makes the task too large, prefer counters first and leave timing for a follow-up.

## 4. Preserve behavior

Instrumentation must not change:

* return values
* cache hit/miss semantics
* invalidation behavior
* exception behavior
* ordering
* thread-safety guarantees

Observance is observational only.

## 5. Low cardinality only

Allowed dimensions/names may include:

* cache region
* operation name

Do not include:

* cache keys
* site keys
* content ids
* content type keys
* actor ids
* request ids
* arbitrary user input

## 6. Runtime integration

Ensure runtime-created cache components receive the runtime `Observance` instance where appropriate.

Preserve no-op defaults.

Existing factory methods should continue to work using `Observance.noop()`.

## 7. Tests

Add focused tests validating:

* hit counter increments on cache hit
* miss counter increments on cache miss
* put/invalidate counters increment when applicable
* behavior remains unchanged
* runtime path uses the supplied `Observance`
* no-op path still works

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

Keep the implementation aligned with ADR-008.

## Notes

This task should focus on making existing cache behavior observable.

Do not redesign the cache API.

Do not introduce TTL, eviction policies, distributed cache support, or cache administration in this task.
