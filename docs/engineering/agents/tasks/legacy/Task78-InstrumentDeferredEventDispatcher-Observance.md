Instrument DeferredEventDispatcher using the Observance API.

Context:
codex-fundamentum now provides the Observance API:

- Observance
- Counter
- Timer
- NoOpObservance
- InMemoryObservance

LocalCodexEventDispatcher is already instrumented with:
- events.dispatched.{EventType}
- subscribers.invoked.{SubscriberType}
- subscribers.duration.{SubscriberType}
- subscribers.failed.{SubscriberType}

ADR-011 documents that Observance is local, lightweight, best-effort, and must not change runtime behavior.

Now we want to instrument DeferredEventDispatcher.

Goal:
Add basic Observance metrics to DeferredEventDispatcher without changing its dispatch, commit, or rollback semantics.

Requirements:
1. Update DeferredEventDispatcher to accept an Observance dependency via constructor injection.

2. Preserve existing constructor behavior by defaulting to:
    - Observance.noop()

3. Do not change transaction semantics.
   If events are currently buffered until commit, keep that behavior.
   If rollback currently discards buffered events, keep that behavior.
   If no transaction is active and events are dispatched immediately, keep that behavior.

4. Add counters for deferred event behavior.

   Suggested metric names:
    - deferred.events.buffered.{EventType}
    - deferred.events.dispatchedImmediately.{EventType}
    - deferred.events.committed
    - deferred.events.discardedOnRollback

5. Add timers where useful but keep it minimal.

   Suggested metric:
    - deferred.commit.duration

   This should measure the time spent dispatching buffered events on commit, if DeferredEventDispatcher owns that loop.

6. If DeferredEventDispatcher does not currently have direct commit/rollback hooks but registers callbacks with TransactionContext, instrument inside those callbacks.

7. Preserve exception behavior.
   Observance must not swallow exceptions.
   If commit dispatch currently propagates exceptions, keep propagating them.
   If it logs/handles them differently, preserve that behavior.

8. Add focused tests using InMemoryObservance:
    - dispatch inside active transaction increments deferred.events.buffered.{EventType}
    - commit increments deferred.events.committed
    - rollback increments deferred.events.discardedOnRollback
    - dispatch outside transaction increments deferred.events.dispatchedImmediately.{EventType}, if that path exists
    - deferred.commit.duration timer records when commit dispatch happens
    - default/noop constructor path still works
    - exception behavior remains unchanged

9. Do not instrument LocalCodexEventDispatcher in this task.
10. Do not instrument cache, index, ChroniconRepository, or service decorators in this task.
11. Do not add Gauge, Histogram, Span, Tags, Labels, tracing, logging, or external metrics backends.
12. Do not change TransactionContext API unless absolutely necessary. If instrumentation requires an API change, stop and report.

Expected result:
- DeferredEventDispatcher emits basic Observance metrics for buffered, committed, rolled-back, and immediate event dispatch behavior.
- Existing transaction/event behavior remains unchanged.
- Tests prove metrics are captured with InMemoryObservance.
- Full reactor build passes.