package codex.index.internal;

/**
 * Metric name constants for {@link ObservingIndexWriter} Observance metrics.
 *
 * <p>All names match the patterns documented in ADR-011. No metric name string may be
 * constructed elsewhere — changes to naming policy belong here.</p>
 */
final class IndexMetricNames {

    static final String UPSERT_CALLS    = "index.upsert.calls";
    static final String UPSERT_DURATION = "index.upsert.duration";
    static final String UPSERT_FAILED  = "index.upsert.failed";

    static final String DELETE_CALLS    = "index.delete.calls";
    static final String DELETE_DURATION = "index.delete.duration";
    static final String DELETE_FAILED  = "index.delete.failed";

    private IndexMetricNames() {}
}
