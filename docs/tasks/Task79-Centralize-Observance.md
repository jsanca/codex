Create a small internal metric-name helper for Observance metrics used by event dispatchers.

Context:
LocalCodexEventDispatcher and DeferredEventDispatcher are now instrumented with Observance.

Metric names are currently built inline as strings.

Goal:
Centralize metric name creation for event-dispatcher Observance metrics without changing generated metric names.

Requirements:
1. Create an internal helper/class for metric names, for example:
    - ObservanceMetricNames
    - CodexMetricNames
    - EventMetricNames

2. Keep it internal/package-private if possible.

3. Add methods/constants for existing metrics:
    - events.dispatched.{EventType}
    - subscribers.invoked.{SubscriberType}
    - subscribers.duration.{SubscriberType}
    - subscribers.failed.{SubscriberType}
    - deferred.events.buffered.{EventType}
    - deferred.events.dispatchedImmediately.{EventType}
    - deferred.events.committed
    - deferred.events.discardedOnRollback
    - deferred.commit.duration

4. Update LocalCodexEventDispatcher and DeferredEventDispatcher to use the helper.

5. Do not change any generated metric name.

6. Add focused tests for the helper:
    - expected event dispatched metric
    - expected subscriber invoked metric
    - expected subscriber duration metric
    - expected subscriber failed metric
    - expected deferred buffered metric
    - expected deferred immediate dispatch metric
    - constants match expected names
    - null event/subscriber names are rejected if applicable

7. Existing Observance tests must still pass without expected name changes.

8. Do not add tags/labels, histograms, gauges, spans, external backends, or new Observance API methods.

Expected result:
- Metric name strings are centralized.
- Existing metrics remain exactly the same.
- Future instrumentation can reuse the same naming style.
- Full reactor build passes.