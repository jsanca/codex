package codex.fundamentum.api.observance;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Records the duration of operations.
 *
 * <p>Operations passed to {@link #record(Runnable)} and {@link #record(Supplier)} are
 * always executed, even in no-op implementations. Implementations must be safe to call
 * from concurrent contexts.</p>
 */
public interface Timer {

    /**
     * Times the given operation and returns its result.
     *
     * @param operation the operation to time; must not be null
     * @param <T>       the result type
     * @return the result of the operation
     */
    <T> T record(Supplier<T> operation);

    /**
     * Times the given operation.
     *
     * @param operation the operation to time; must not be null
     */
    void record(Runnable operation);

    /**
     * Records an explicit duration, typically measured externally.
     *
     * @param duration the duration to record; must not be null
     */
    void record(Duration duration);
}
