package codex.fundamentum.api.observance;

/**
 * A monotonically increasing counter.
 *
 * <p>Counters measure cumulative totals — event counts, invocation counts, failure counts.
 * They only increase. Implementations must be safe to call from concurrent contexts.</p>
 */
public interface Counter {

    /**
     * Increments the counter by one.
     */
    void increment();

    /**
     * Increments the counter by the given amount.
     *
     * @param amount the amount to add; must be greater than zero
     * @throws IllegalArgumentException if {@code amount} is less than one
     */
    void increment(long amount);
}
