package codex.fundamentum.api.event;

import java.util.Objects;

/**
 * Metric name factory for {@link LocalCodexEventDispatcher} Observance metrics.
 *
 * <p>All methods produce names that match the patterns documented in ADR-011. No metric name
 * string may be constructed elsewhere — changes to naming policy belong here.</p>
 */
final class EventDispatchMetricNames {

    private EventDispatchMetricNames() {}

    /**
     * @param eventSimpleName {@link Class#getSimpleName()} of the dispatched event; must not be null
     * @return {@code events.dispatched.{eventSimpleName}}
     */
    static String eventsDispatched(final String eventSimpleName) {
        Objects.requireNonNull(eventSimpleName, "eventSimpleName must not be null");
        return "events.dispatched." + eventSimpleName;
    }

    /**
     * @param subscriberSimpleName {@link Class#getSimpleName()} of the subscriber; must not be null
     * @return {@code subscribers.invoked.{subscriberSimpleName}}
     */
    static String subscribersInvoked(final String subscriberSimpleName) {
        Objects.requireNonNull(subscriberSimpleName, "subscriberSimpleName must not be null");
        return "subscribers.invoked." + subscriberSimpleName;
    }

    /**
     * @param subscriberSimpleName {@link Class#getSimpleName()} of the subscriber; must not be null
     * @return {@code subscribers.duration.{subscriberSimpleName}}
     */
    static String subscribersDuration(final String subscriberSimpleName) {
        Objects.requireNonNull(subscriberSimpleName, "subscriberSimpleName must not be null");
        return "subscribers.duration." + subscriberSimpleName;
    }

    /**
     * @param subscriberSimpleName {@link Class#getSimpleName()} of the subscriber; must not be null
     * @return {@code subscribers.failed.{subscriberSimpleName}}
     */
    static String subscribersFailed(final String subscriberSimpleName) {
        Objects.requireNonNull(subscriberSimpleName, "subscriberSimpleName must not be null");
        return "subscribers.failed." + subscriberSimpleName;
    }
}
