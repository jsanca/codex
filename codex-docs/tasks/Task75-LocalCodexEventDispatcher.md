Instrument LocalCodexEventDispatcher using the new Observance API.

Context:
Task 74 introduced a minimal framework-neutral observance API in codex-fundamentum:

- Observance
- Counter
- Timer
- NoOpObservance
- InMemoryObservance

Now we want to instrument the local event dispatch pipeline.

LocalCodexEventDispatcher delivers Codex domain events to local subscribers. It is the first good instrumentation target because Chronicon, indexing, cache invalidation, and future internal subscribers all pass through it.

Goal:
Add basic observability to LocalCodexEventDispatcher without changing event dispatch behavior.

Requirements:
1. Update LocalCodexEventDispatcher to accept an Observance dependency via constructor injection.

2. Preserve a default constructor or factory path using:
    - Observance.noop()
      or
    - NoOpObservance instance
      depending on the API style from Task 74.

3. Track event dispatch count by event type.

   Suggested metric name:
    - events.dispatched.{eventSimpleName}

   Example:
    - events.dispatched.ContentItemPublishedEvent

4. Track subscriber invocation count by subscriber type.

   Suggested metric name:
    - subscribers.invoked.{subscriberSimpleName}

5. Track subscriber failure count by subscriber type.

   Suggested metric name:
    - subscribers.failed.{subscriberSimpleName}

6. Track subscriber execution duration by subscriber type.

   Suggested metric name:
    - subscribers.duration.{subscriberSimpleName}

7. Ensure subscriber duration is recorded for both successful and failing subscribers.

8. Preserve existing exception behavior.
   If LocalCodexEventDispatcher currently propagates subscriber exceptions, keep propagating them.
   If it currently swallows/logs exceptions, keep that behavior.
   Do not change dispatch semantics in this task.

9. Add focused tests using InMemoryObservance:
    - dispatching an event increments events.dispatched.{eventSimpleName}
    - invoking a matching subscriber increments subscribers.invoked.{subscriberSimpleName}
    - subscriber duration count increments
    - failing subscriber increments subscribers.failed.{subscriberSimpleName}
    - failure behavior remains the same as before
    - NoOpObservance/default constructor path still works

10. Do not instrument DeferredEventDispatcher in this task.
11. Do not instrument CompositeCodexEventDispatcher in this task.
12. Do not instrument cache, Chronicon repositories, index writers, or transaction context in this task.
13. Do not introduce tags/labels, histograms, gauges, spans, tracing, or external dependencies.
14. Do not change subscriber registration or runtime wiring unless required to pass the new Observance dependency.

Expected result:
- LocalCodexEventDispatcher emits basic event/subscriber metrics.
- Existing event dispatch behavior is unchanged.
- Tests prove metrics are captured through InMemoryObservance.
- Full reactor build passes.