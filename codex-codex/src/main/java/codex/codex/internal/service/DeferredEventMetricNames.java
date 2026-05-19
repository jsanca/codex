package codex.codex.internal.service;

import java.util.Objects;

/**
 * Metric name factory for {@link DeferredEventDispatcher} Observance metrics.
 *
 * <p>All methods and constants produce names that match the patterns documented in ADR-011.
 * No metric name string may be constructed elsewhere — changes to naming policy belong here.</p>
 */
final class DeferredEventMetricNames {

    /** Total events dispatched on transaction commit. */
    static final String COMMITTED = "deferred.events.committed";

    /** Total events discarded when a transaction rolls back. */
    static final String DISCARDED_ON_ROLLBACK = "deferred.events.discardedOnRollback";

    /** Duration of the full commit dispatch loop. */
    static final String COMMIT_DURATION = "deferred.commit.duration";

    private DeferredEventMetricNames() {}

    /**
     * @param eventSimpleName {@link Class#getSimpleName()} of the buffered event; must not be null
     * @return {@code deferred.events.buffered.{eventSimpleName}}
     */
    static String buffered(final String eventSimpleName) {
        Objects.requireNonNull(eventSimpleName, "eventSimpleName must not be null");
        return "deferred.events.buffered." + eventSimpleName;
    }

    /**
     * @param eventSimpleName {@link Class#getSimpleName()} of the dispatched event; must not be null
     * @return {@code deferred.events.dispatchedImmediately.{eventSimpleName}}
     */
    static String dispatchedImmediately(final String eventSimpleName) {
        Objects.requireNonNull(eventSimpleName, "eventSimpleName must not be null");
        return "deferred.events.dispatchedImmediately." + eventSimpleName;
    }
}
