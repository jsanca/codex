Add a minimal Observance API to codex-fundamentum.

Context:
Codex now has several event-driven runtime concerns that will need observability:

- Event dispatching
- Subscriber invocation/failure/duration
- Deferred event commit/rollback behavior
- Cache hit/miss/invalidation
- Chronicon audit writes
- Index upserts/deletes

Before instrumenting any concrete component, we want a tiny framework-neutral Observance API.

Goal:
Create the minimal Observance foundation in codex-fundamentum without instrumenting production components yet.

Requirements:
1. Create package:
    - codex.fundamentum.api.observance

2. Add the package export to:
    - module-info.java

3. Create a minimal API with:
    - Observance
    - Counter
    - Timer
    - NoOpObservance
    - InMemoryObservance

4. Keep the API framework-neutral:
    - no Micrometer
    - no OpenTelemetry
    - no Prometheus
    - no Spring
    - no external dependencies

5. Suggested API shape:

   Observance:
    - Counter counter(String name)
    - Timer timer(String name)

   Counter:
    - void increment()
    - void increment(long amount)

   Timer:
    - <T> T record(Supplier<T> operation)
    - void record(Runnable operation)
    - void record(Duration duration)

   Adjust names if the existing project style suggests better naming.

6. NoOpObservance:
    - returns no-op counters and timers
    - should be safe to use as the default everywhere
    - should not allocate unnecessary state

7. InMemoryObservance:
    - stores counters and timer measurements in memory
    - intended for tests and development visibility
    - should be thread-safe enough for basic concurrent use
    - use AtomicLong or similar primitives where appropriate

8. Provide read methods on InMemoryObservance useful for tests, for example:
    - long counterValue(String name)
    - long timerCount(String name)
    - Duration timerTotalDuration(String name)

   Exact method names can follow project style.

9. Consider adding reset() only if it keeps tests simpler.
   Do not overbuild.

10. Add unit tests covering:
- counter starts at zero
- counter increment()
- counter increment(amount)
- timer records Runnable execution
- timer records Supplier execution and returns the result
- timer records explicit Duration
- NoOpObservance counter/timer operations do not fail
- repeated calls to counter(name) refer to the same logical counter
- repeated calls to timer(name) refer to the same logical timer
- null metric names are rejected, if consistent with project style
- invalid counter increments, such as negative amounts, are rejected or explicitly documented

11. Do not instrument LocalCodexEventDispatcher in this task.
12. Do not modify cache, Chronicon, index, transaction, or runtime wiring code.
13. Do not introduce metric-name constants yet unless needed by the API tests.
14. Keep the API small. Do not add Gauge, Histogram, Span, Trace, Tags, Labels, or HealthCheck in this task.

Expected result:
- codex-fundamentum exposes a small observance API.
- No production component is instrumented yet.
- InMemoryObservance can be used in future tests.
- NoOpObservance can be used as a safe default.
- All tests pass.