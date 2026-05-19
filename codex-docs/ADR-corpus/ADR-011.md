# ADR-011: Observance — Metrics Foundation

## Status

Accepted

## Context

Codex now has a meaningful event-driven runtime. Core services publish domain events through
`EventPublishing` decorators; `DeferredEventDispatcher` buffers them until transaction commit;
`LocalCodexEventDispatcher` delivers them synchronously to in-process subscribers. Chronicon
audit subscribers, `codex-index` indexing subscribers, and ContentItem cache invalidation
subscribers all receive events through this pipeline.

As the runtime grows, understanding its behavior in production requires more than log lines.
We need lightweight metrics to answer questions like: are events being dispatched? which
subscribers are slow? how often do subscribers fail?

Observance is introduced as a framework-neutral metrics abstraction that lives in
`codex-fundamentum` and is usable by any module — without depending on Micrometer,
Prometheus, OpenTelemetry, or any Spring infrastructure.

---

## Decision

**Codex starts with a minimal Observance API: `Counter` and `Timer` only.**

Observance is local, lightweight, and best-effort. It measures counts and durations.
It does not replace Chronicon audit, technical logging, or security logging. It does not
coordinate across nodes.

The API is intentionally narrow. Every additional instrument type (Gauge, Histogram, Span)
must be justified by a concrete instrumentation need, not added speculatively.

---

## Observance API

Package: `codex.fundamentum.api.observance`

| Type | Role |
|------|------|
| `Observance` | Entry point — provides named counters and timers |
| `Counter` | Monotonically increasing count |
| `Timer` | Duration recording |
| `NoOpObservance` | Safe default — discards all observations, zero allocation |
| `InMemoryObservance` | In-memory accumulator — for tests and development visibility |

### `Observance`

```java
Counter counter(String name);   // same name → same logical counter
Timer   timer(String name);     // same name → same logical timer
static Observance noop();       // returns NoOpObservance singleton
```

### `Counter`

```java
void increment();               // +1
void increment(long amount);    // +amount; amount must be ≥ 1
```

### `Timer`

```java
<T> T record(Supplier<T> operation);   // times and returns result
void  record(Runnable operation);       // times void operation
void  record(Duration duration);        // records externally measured duration
```

Operations passed to `Timer` are always executed — even in `NoOpObservance`. Only the
measurement is discarded.

### Read-back (InMemoryObservance only, for tests)

```java
long     counterValue(String name);
long     timerCount(String name);
Duration timerTotalDuration(String name);
```

---

## Current Instrumentation

`LocalCodexEventDispatcher` is the first instrumented component. It accepts an `Observance`
via constructor injection and defaults to `Observance.noop()` when none is provided.

### Metrics emitted

| Metric | Type | When |
|--------|------|------|
| `events.dispatched.{EventSimpleName}` | Counter | Once per `dispatch()` call |
| `subscribers.invoked.{SubscriberSimpleName}` | Counter | Once per matching subscriber |
| `subscribers.duration.{SubscriberSimpleName}` | Timer | Per invocation — success and failure |
| `subscribers.failed.{SubscriberSimpleName}` | Counter | When a subscriber throws |

### Metric semantics

- `events.dispatched` increments once per `dispatch()` call, regardless of how many
  subscribers match.
- `subscribers.invoked` increments once per subscriber that receives the event. An event
  dispatched to two matching subscribers increments the counter twice (once per subscriber).
- `subscribers.duration` is recorded on both the success and failure paths. The `Timer`
  implementation uses `try-finally` to guarantee this.
- `subscribers.failed` increments only when the subscriber's `handle()` throws a
  `RuntimeException`. The exception is rethrown unchanged — Observance does not swallow it.
- Observance must not change dispatch behavior. The event pipeline behaves identically
  whether `NoOpObservance` or `InMemoryObservance` is injected.

---

## Cluster Semantics

Counters and timers are local to the current process. `InMemoryObservance` uses
`AtomicLong` — safe for concurrent use within a JVM, not coordinated across nodes.

- Do not use `Redis INCR` or any synchronous remote operation inside the hot dispatch path.
- Do not use distributed locks for counter updates.
- Cluster-wide metric aggregation (sum across nodes, rate calculation, alerting) belongs
  to a future external metrics backend (Micrometer + Prometheus, OpenTelemetry Collector,
  or equivalent). The `Observance` interface is narrow enough that an adapter can bridge
  it to any backend without callers changing.

---

## Boundaries

Observance is not a substitute for Chronicon audit or for technical logging. Each serves
a distinct purpose:

| Layer | What it records | Audience |
|-------|-----------------|----------|
| **Chronicon** | Business/domain facts — what happened to which entity, who did it, when | Compliance, audit trail, business history |
| **Logs** (SLF4J) | Technical and security narrative — what the system did, why decisions were made, errors | Engineers, on-call, security |
| **Observance** | Counts and durations — how often, how fast, how many failures | Operations, SRE, dashboards, alerting |

These three layers are complementary and must not bleed into each other. Observance does
not store business history. Chronicon does not record timing. Logs do not aggregate counts.

---

## Known Limitations and Future Follow-ups

### No external metrics backend

`InMemoryObservance` has no export path. Metrics are visible within the JVM only.
A future `MicrometerObservance` or `OpenTelemetryObservance` adapter would implement
`Observance` and bridge to a Prometheus scrape endpoint or OTLP exporter.

### No tags or labels

Metric names currently embed the event or subscriber type directly
(`events.dispatched.ContentItemPublishedEvent`). A tag-based model
(`events.dispatched` tagged with `eventType=ContentItemPublishedEvent`) is more composable
and is standard in Micrometer and OpenTelemetry. Tags require a richer `Counter`/`Timer`
API. Not in scope until a backend adapter exists.

### No histograms or percentiles

`Timer` records total duration and count. P50/P95/P99 latency requires a histogram or
summary instrument. Not in scope until query requirements exist.

### No correlation or eventId

Events dispatched through `LocalCodexEventDispatcher` carry no correlation id. Cross-event
tracing (e.g. linking a `ContentItemPublishedEvent` dispatch to its downstream index write
and audit record write) requires a trace context propagation mechanism. Not in scope.

### DeferredEventDispatcher not instrumented

`DeferredEventDispatcher` buffers events until transaction commit and discards them on
rollback. Commit/rollback rates and deferred event counts are not yet measured. A future
task should add:

```
deferred.events.committed
deferred.events.discarded
```

### Cache not instrumented

`CacheRegion` has no observability hooks. A future `ObservingCacheRegion<K,V>` decorator
could record hit rate, miss rate, load duration, and eviction count. `CaffeineCacheRegion`
has native stats support via `Caffeine.recordStats()` that could be bridged.

### IndexWriter not instrumented

`IndexWriter.upsert()` and `IndexWriter.delete()` are not timed. A future
`ObservingIndexWriter` decorator would record upsert/delete counts and durations.

### ChroniconRepository not instrumented

`ChroniconRepository.save()` and query methods are not timed. Audit write latency is
currently invisible.

### Transaction metrics not instrumented

`TransactionContext` commit and rollback callbacks are not measured. Rollback rate is
a useful early-warning signal for data consistency issues.

### Future timed service decorators

The service decorator pattern used throughout Codex
(`EventPublishingSiteService → CachingSiteService → CodexSiteService`) is the natural
place to measure caller-visible operation latency. Future timed decorators would add one
concern — timing — without touching business logic:

```
TimedSiteService          → wraps SiteService
TimedContentTypeService   → wraps ContentTypeService
TimedContentItemService   → wraps ContentItemService
```

These would be implemented by forwarding each method call through `Timer.record(Supplier<T>)`,
consistent with the existing decorator pattern. No AOP, no proxies, no reflection.
