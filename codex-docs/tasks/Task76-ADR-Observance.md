Create an ADR documenting the initial Observance baseline for Codex.

Context:
codex-fundamentum now exposes a minimal Observance API:

- Observance
- Counter
- Timer
- NoOpObservance
- InMemoryObservance

LocalCodexEventDispatcher is now instrumented with:

- events.dispatched.{EventType}
- subscribers.invoked.{SubscriberType}
- subscribers.duration.{SubscriberType}
- subscribers.failed.{SubscriberType}

Codex already has an event-driven runtime with:
- EventPublishing service decorators
- DeferredEventDispatcher
- LocalCodexEventDispatcher
- Chronicon audit subscribers
- codex-index subscribers
- ContentItem cache invalidation subscribers

Goal:
Document the current Observance design, semantics, boundaries, and future follow-ups.

Please include:

1. Status
    - Accepted

2. Context
    - Codex now has event-driven infrastructure.
    - Audit, indexing, and cache invalidation are handled by subscribers.
    - We need lightweight metrics to understand runtime behavior.
    - Observance is introduced as a framework-neutral metrics abstraction.

3. Decision
    - Observance starts with Counter and Timer only.
    - No Gauge, Histogram, Span, Trace, Tags, Labels, MDC, or external backend yet.
    - NoOpObservance is the safe default.
    - InMemoryObservance is for tests and local/development visibility.
    - Observance must be local, lightweight, and best-effort.
    - Metrics are local observations, not distributed coordination.

4. Observance API
    - Observance.counter(name)
    - Observance.timer(name)
    - Counter.increment()
    - Counter.increment(amount)
    - Timer.record(Runnable)
    - Timer.record(Supplier<T>)
    - Timer.record(Duration)

5. Current instrumentation
    - LocalCodexEventDispatcher is the first instrumented component.
    - events.dispatched.{EventType}
    - subscribers.invoked.{SubscriberType}
    - subscribers.duration.{SubscriberType}
    - subscribers.failed.{SubscriberType}

6. Metric semantics
    - events.dispatched increments once per dispatch() call.
    - subscribers.invoked increments once per matching subscriber invocation.
    - subscribers.duration records both success and failure paths.
    - subscribers.failed increments when a subscriber throws.
    - Observance must not swallow exceptions.
    - Observance must not change dispatch behavior.

7. Cluster semantics
    - Counters and timers are local to the process/node.
    - Cluster-wide aggregation belongs to a future metrics backend.
    - Do not use distributed locks for counters.
    - Do not use Redis INCR or other synchronous remote coordination inside the hot path.
    - Observance should not block domain operations.

8. Boundaries
    - Observance is not Chronicon audit.
    - Observance is not technical logging.
    - Observance is not security logging.
    - Observance does not store business history.
    - Chronicon records business/domain audit.
    - Logs narrate technical/security behavior.
    - Observance measures counts and durations.

9. Known limitations / future follow-ups
    - No external metrics backend yet.
    - No tags/labels yet.
    - No histograms or percentiles yet.
    - No eventId/correlation id yet.
    - DeferredEventDispatcher is not instrumented yet.
    - Cache metrics are not instrumented yet.
    - IndexWriter metrics are not instrumented yet.
    - ChroniconRepository metrics are not instrumented yet.
    - Transaction commit/rollback metrics are not instrumented yet.
    - Future timed service decorators may measure caller-visible service operation cost.

10. Explicitly mention future timed service decorators as future work:
- TimedSiteService
- TimedContentTypeService
- TimedContentItemService
- implemented via existing Forwarding service pattern
- no AOP/proxies/reflection for now

Constraints:
- Documentation only.
- Do not change production code.
- Do not change tests.
- Do not add dependencies.
- Follow the existing ADR numbering/style.