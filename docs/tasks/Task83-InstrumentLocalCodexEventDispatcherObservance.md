# Task — Instrument LocalCodexEventDispatcher with Observance

## Goal

Add the first production-grade Observance instrumentation to the Codex event pipeline.

This task establishes the baseline event dispatch metrics and validates the architectural direction defined in ADR-008.

---

# Scope

Instrument `LocalCodexEventDispatcher` (and related event pipeline components if necessary) with Observance support.

The instrumentation must measure:

* dispatched event count
* failed dispatch count
* dispatch duration

The implementation must remain:

* lightweight
* implementation-agnostic
* explicit
* low-cardinality

---

# Requirements

## 1. Constructor-based Observance injection

`LocalCodexEventDispatcher` should receive an `Observance` instance explicitly.

A no-op default path is acceptable if consistent with existing patterns.

Avoid:

* static globals
* service locators
* reflection
* annotations
* AOP

---

## 2. Dispatch duration metrics

Measure total dispatch duration for each event dispatch operation.

Use a semantic metric naming convention consistent with ADR-008.

Example direction (not mandatory):

```text
codex.event.dispatch.duration
```

---

## 3. Success and failure counters

Record:

* successful dispatches
* failed dispatches

Potential examples:

```text
codex.event.dispatch.success
codex.event.dispatch.failure
```

---

## 4. Allowed tags only

Allowed low-cardinality tags may include:

* event type
* dispatch mode

Do NOT introduce tags for:

* aggregate ids
* content ids
* site ids
* actor ids
* request ids
* arbitrary payload values

---

## 5. Preserve existing behavior

Instrumentation must not change:

* dispatch semantics
* ordering
* exception behavior
* sync/async behavior
* transaction semantics

Observance is observational only.

---

## 6. Add tests

Add focused tests using:

* `RecordingObservance`
* fake timers/counters if needed

Validate:

* duration metrics recorded on success
* failure counters recorded on exceptions
* existing dispatch behavior preserved

Avoid testing vendor integrations.

---

# Constraints

Do NOT introduce:

* Micrometer
* Prometheus
* OpenTelemetry
* Spring dependencies
* proxies
* annotations
* reflection-based instrumentation

Keep the implementation aligned with Codex explicit decorator/composition philosophy.

---

# Notes

Dispatcher observance measures:

* event pipeline execution

This is intentionally different from future timed service decorators, which measure:

* caller-visible domain operation costs

Keep those responsibilities conceptually separated.
