Add an Observance-aware ConciliumRuntime in-memory factory.

Context:
Observance is now integrated into:
- LocalCodexEventDispatcher
- DeferredEventDispatcher
- IndexRuntime via ObservingIndexWriter

IndexRuntime now has Observance-aware construction, while existing factories default to Observance.noop().

Goal:
Provide a simple runtime construction path that wires the same Observance instance through the full in-memory Concilium stack.

Requirements:
1. Inspect ConciliumRuntime construction and existing inMemory factories.

2. Add a new factory method, for example:
    - ConciliumRuntime.inMemory(Observance observance)

3. Validate observance is not null.

4. Preserve the existing:
    - ConciliumRuntime.inMemory()

   It should delegate to:
    - ConciliumRuntime.inMemory(Observance.noop())

5. Ensure the same Observance instance is passed to:
    - IndexRuntime / ObservingIndexWriter
    - LocalCodexEventDispatcher
    - DeferredEventDispatcher

   if those components are constructed inside ConciliumRuntime.

6. Do not change runtime semantics.
   The only change should be that metrics can be captured when a non-noop Observance is provided.

7. Add integration tests using InMemoryObservance proving that a normal publish flow captures metrics across layers:
    - deferred.events.buffered.{EventType} or deferred.events.dispatchedImmediately.{EventType}, depending on the runtime flow
    - events.dispatched.{EventType}
    - subscribers.invoked.{SubscriberType}
    - subscribers.duration.{SubscriberType}
    - index.upsert.calls
    - index.upsert.duration

8. Add a test for an unpublish/archive/delete flow if there is already a convenient integration path:
    - index.delete.calls
    - index.delete.duration

9. Existing tests using ConciliumRuntime.inMemory() must continue to pass unchanged or with minimal expected-count updates if existing behavior was already being asserted.

10. Do not add new metrics.
11. Do not instrument ChroniconRepository or CacheRegion in this task.
12. Do not add external dependencies.

Expected result:
- A single Observance instance can observe the full in-memory Concilium runtime.
- Existing no-op runtime construction remains backward compatible.
- Tests prove metrics are emitted across event dispatch and index writer layers.
- Full reactor build passes.