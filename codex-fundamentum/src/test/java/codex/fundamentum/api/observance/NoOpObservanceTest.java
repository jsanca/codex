package codex.fundamentum.api.observance;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NoOpObservance}.
 */
class NoOpObservanceTest {

    private final Observance observance = Observance.noop();

    // --- factory ---

    @Test
    void noopFactoryReturnsNoOpObservance() {
        assertInstanceOf(NoOpObservance.class, Observance.noop());
    }

    @Test
    void noopFactoryReturnsSameInstance() {
        assertSame(Observance.noop(), Observance.noop());
    }

    // --- counter: null guard ---

    @Test
    void counterRejectsNullName() {
        assertThrows(NullPointerException.class, () -> observance.counter(null));
    }

    // --- counter: no-op operations do not throw ---

    @Test
    void counterIncrementDoesNotThrow() {
        assertDoesNotThrow(() -> observance.counter("x").increment());
    }

    @Test
    void counterIncrementByAmountDoesNotThrow() {
        assertDoesNotThrow(() -> observance.counter("x").increment(5));
    }

    @Test
    void repeatedCounterCallsReturnSameInstance() {
        assertSame(observance.counter("a"), observance.counter("a"));
    }

    // --- timer: null guard ---

    @Test
    void timerRejectsNullName() {
        assertThrows(NullPointerException.class, () -> observance.timer(null));
    }

    // --- timer: operations are still executed ---

    @Test
    void timerRunnableIsExecuted() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        observance.timer("t").record(() -> executed.set(true));
        assertTrue(executed.get(), "runnable must be executed even in no-op timer");
    }

    @Test
    void timerSupplierIsExecutedAndResultReturned() {
        final String result = observance.timer("t").record(() -> "value");
        assertEquals("value", result);
    }

    @Test
    void timerRecordDurationDoesNotThrow() {
        assertDoesNotThrow(() -> observance.timer("t").record(Duration.ofMillis(10)));
    }

    @Test
    void timerRunnableRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> observance.timer("t").record((Runnable) null));
    }

    @Test
    void timerSupplierRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> observance.timer("t").record((java.util.function.Supplier<?>) null));
    }

    @Test
    void repeatedTimerCallsReturnSameInstance() {
        assertSame(observance.timer("a"), observance.timer("a"));
    }
}
