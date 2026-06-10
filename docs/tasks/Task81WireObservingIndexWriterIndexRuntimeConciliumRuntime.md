Wire ObservingIndexWriter into the index runtime.

Context:
Task 80 added ObservingIndexWriter as a decorator over IndexWriter.

It records:
- index.upsert.calls
- index.upsert.duration
- index.upsert.failures
- index.delete.calls
- index.delete.duration
- index.delete.failures

Goal:
Activate index writer metrics in the runtime by wrapping the existing IndexWriter with ObservingIndexWriter.

Requirements:
1. Inspect IndexRuntime and ConciliumRuntime construction.
2. Add an Observance-aware construction path where appropriate.
3. Preserve existing constructors/factory methods by defaulting to Observance.noop().
4. Wrap the existing IndexWriter with ObservingIndexWriter exactly once.
5. Ensure existing ContentItem indexing subscribers use the observed writer.
6. Do not change IndexWriter interface.
7. Do not change indexing subscriber behavior.
8. Do not change IndexDocument mapping.
9. Add focused tests using InMemoryObservance proving:
    - publish flow increments index.upsert.calls
    - publish flow records index.upsert.duration
    - unpublish/archive/delete flow increments index.delete.calls if such runtime integration tests exist
10. Existing tests should continue to pass.
11. Do not instrument cache or ChroniconRepository in this task.
12. Do not add external dependencies.

Expected result:
- IndexWriter metrics are active in the index runtime.
- Existing no-observance runtime path still works through Observance.noop().
- Full reactor build passes.