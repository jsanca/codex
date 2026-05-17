package codex.fundamentum.api.observance;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A no-op {@link Observance} that discards all observations without allocating metric state.
 *
 * <p>Suitable as a safe default wherever an {@link Observance} is required but no collection
 * is desired. Operations passed to {@link Timer} methods are still executed normally — only
 * the timing measurement is discarded.</p>
 *
 * <p>Obtain via {@link Observance#noop()}.</p>
 */
public final class NoOpObservance implements Observance {

    static final NoOpObservance INSTANCE = new NoOpObservance();

    private static final Counter NOOP_COUNTER = new Counter() {
        @Override public void increment() {}
        @Override public void increment(final long amount) {}
    };

    private static final Timer NOOP_TIMER = new Timer() {
        @Override
        public <T> T record(final Supplier<T> operation) {
            Objects.requireNonNull(operation, "operation must not be null");
            return operation.get();
        }

        @Override
        public void record(final Runnable operation) {
            Objects.requireNonNull(operation, "operation must not be null");
            operation.run();
        }

        @Override
        public void record(final Duration duration) {}
    };

    private NoOpObservance() {}

    @Override
    public Counter counter(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        return NOOP_COUNTER;
    }

    @Override
    public Timer timer(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        return NOOP_TIMER;
    }
}
