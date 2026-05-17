package codex.fundamentum.api.observance;

/**
 * Entry point for Codex observability.
 *
 * <p>An {@code Observance} provides named {@link Counter}s and {@link Timer}s.
 * The same name always refers to the same logical instrument: calling
 * {@code counter("events.dispatched")} twice returns counters that share state.</p>
 *
 * <p>The API is intentionally small and framework-neutral. Future adapters for
 * Micrometer, OpenTelemetry, or Prometheus can implement this interface without
 * changing callers.</p>
 *
 * <p>Use {@link #noop()} as a safe default that records nothing and allocates no state.</p>
 */
public interface Observance {

    /**
     * Returns the counter with the given name.
     * Repeated calls with the same name return the same logical counter.
     *
     * @param name the metric name; must not be null
     * @return the counter for the given name; never null
     */
    Counter counter(String name);

    /**
     * Returns the timer with the given name.
     * Repeated calls with the same name return the same logical timer.
     *
     * @param name the metric name; must not be null
     * @return the timer for the given name; never null
     */
    Timer timer(String name);

    /**
     * Returns a no-op {@code Observance} that discards all observations.
     * Safe to use as a default in any context.
     *
     * @return the no-op observance singleton
     */
    static Observance noop() {
        return NoOpObservance.INSTANCE;
    }
}
