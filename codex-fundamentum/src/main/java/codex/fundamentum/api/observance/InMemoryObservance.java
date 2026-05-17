package codex.fundamentum.api.observance;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * An in-memory {@link Observance} that accumulates counter and timer measurements.
 *
 * <p>Intended for use in tests and development visibility. Instruments are stored by name
 * in concurrent maps; all operations are thread-safe for basic concurrent use.</p>
 *
 * <p>Read back accumulated values via {@link #counterValue(String)},
 * {@link #timerCount(String)}, and {@link #timerTotalDuration(String)}.</p>
 */
public final class InMemoryObservance implements Observance {

    private final ConcurrentHashMap<String, InMemoryCounter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InMemoryTimer> timers = new ConcurrentHashMap<>();

    @Override
    public Counter counter(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        return counters.computeIfAbsent(name, k -> new InMemoryCounter());
    }

    @Override
    public Timer timer(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        return timers.computeIfAbsent(name, k -> new InMemoryTimer());
    }

    /**
     * Returns the current value of the named counter, or zero if it has never been incremented.
     *
     * @param name the counter name; must not be null
     * @return the current count
     */
    public long counterValue(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        final InMemoryCounter counter = counters.get(name);
        return counter == null ? 0L : counter.count.get();
    }

    /**
     * Returns the number of recordings for the named timer, or zero if it has never been recorded.
     *
     * @param name the timer name; must not be null
     * @return the number of recordings
     */
    public long timerCount(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        final InMemoryTimer timer = timers.get(name);
        return timer == null ? 0L : timer.count.get();
    }

    /**
     * Returns the total accumulated duration for the named timer, or {@link Duration#ZERO} if
     * it has never been recorded.
     *
     * @param name the timer name; must not be null
     * @return the total duration
     */
    public Duration timerTotalDuration(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        final InMemoryTimer timer = timers.get(name);
        return timer == null ? Duration.ZERO : Duration.ofNanos(timer.totalNanos.get());
    }

    // --- inner types ---

    private static final class InMemoryCounter implements Counter {

        private final AtomicLong count = new AtomicLong(0);

        @Override
        public void increment() {
            count.incrementAndGet();
        }

        @Override
        public void increment(final long amount) {
            if (amount < 1) {
                throw new IllegalArgumentException("amount must be greater than zero, was: " + amount);
            }
            count.addAndGet(amount);
        }
    }

    private static final class InMemoryTimer implements Timer {

        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalNanos = new AtomicLong(0);

        @Override
        public <T> T record(final Supplier<T> operation) {
            Objects.requireNonNull(operation, "operation must not be null");
            final long start = System.nanoTime();
            try {
                return operation.get();
            } finally {
                recordNanos(System.nanoTime() - start);
            }
        }

        @Override
        public void record(final Runnable operation) {
            Objects.requireNonNull(operation, "operation must not be null");
            final long start = System.nanoTime();
            try {
                operation.run();
            } finally {
                recordNanos(System.nanoTime() - start);
            }
        }

        @Override
        public void record(final Duration duration) {
            Objects.requireNonNull(duration, "duration must not be null");
            recordNanos(duration.toNanos());
        }

        private void recordNanos(final long nanos) {
            totalNanos.addAndGet(nanos);
            count.incrementAndGet();
        }
    }
}
