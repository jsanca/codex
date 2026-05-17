Inspect the current project and propose a minimal Observance/metrics foundation for Codex.

Context:
Codex now has a meaningful event-driven runtime:

- Core services publish lifecycle events through EventPublishing decorators.
- DeferredEventDispatcher buffers events until commit.
- LocalCodexEventDispatcher delivers events to subscribers.
- Chronicon writes AuditRecord entries for Site, ContentType, and ContentItem lifecycle events.
- codex-index handles ContentItem public indexing.
- ContentItem cache invalidation is event-driven.

ADR-010 documents Chronicon audit coverage semantics.
ADR-008 documents ContentItem lifecycle semantics.
There is also an indexing ADR documenting published-only ContentItem indexing semantics.

Goal:
Inspect the codebase and recommend the smallest safe Observance/metrics foundation.

Please check:

1. Existing observability/metrics code
    - Is there already a module/package for observability, observance, telemetry, metrics, or monitoring?
    - Are there any existing interfaces for counters, timers, gauges, spans, events, or health checks?
    - Is SLF4J currently the only observability mechanism?

2. Good first instrumentation points
   Inspect and rank these possible first targets:
    - CodexEventDispatcher / LocalCodexEventDispatcher
    - DeferredEventDispatcher
    - ChroniconRepository or Chronicon subscribers
    - IndexWriter
    - CacheRegion / CachingContentItemService
    - TransactionContext

3. Minimal API shape
   Recommend a tiny internal API, for example:
    - Observance
    - CodexMetrics
    - Counter
    - Timer
    - NoopObservance
    - InMemoryObservance

   Keep it framework-neutral. Do not depend on Prometheus, Micrometer, Spring, or OpenTelemetry yet.

4. Module placement
   Recommend where this belongs:
    - codex-fundamentum
    - new codex-observance module
    - internal package inside codex-codex

5. Event pipeline metrics
   Propose the smallest useful first metrics:
    - events dispatched by type
    - subscriber invocation count
    - subscriber failures
    - subscriber duration
    - deferred events committed
    - deferred events discarded on rollback

6. Cache metrics
   Identify whether CacheRegion already exposes enough hooks for:
    - get
    - hit
    - miss
    - load
    - eviction/invalidation

7. Recommended first implementation task
   Propose a small task that adds the minimal Observance API and instruments one area only.

Constraints:
- Inspection/report only.
- Do not implement the Observance API yet.
- Do not add dependencies.
- Do not change production code unless fixing a typo.
- Do not modify tests.

Expected output:
- Current observability baseline.
- Recommended module/package placement.
- Minimal API proposal.
- First instrumentation target recommendation.
- Risks and future follow-ups.