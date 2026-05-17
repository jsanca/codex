package codex.fundamentum.api.observance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link InMemoryObservance}.
 */
class InMemoryObservanceTest {

    private InMemoryObservance observance;

    @BeforeEach
    void setUp() {
        observance = new InMemoryObservance();
    }

    // --- counter: null guard ---

    @Test
    void counterRejectsNullName() {
        assertThrows(NullPointerException.class, () -> observance.counter(null));
    }

    // --- counter: identity ---

    @Test
    void repeatedCounterCallsReturnSameLogicalCounter() {
        observance.counter("events").increment();
        observance.counter("events").increment();
        assertEquals(2, observance.counterValue("events"));
    }

    // --- counter: initial value ---

    @Test
    void counterStartsAtZero() {
        assertEquals(0, observance.counterValue("new-counter"));
    }

    @Test
    void counterValueReturnsZeroForUntouchedName() {
        observance.counter("other");
        assertEquals(0, observance.counterValue("untouched"));
    }

    // --- counter: increment() ---

    @Test
    void counterIncrementByOne() {
        observance.counter("clicks").increment();
        assertEquals(1, observance.counterValue("clicks"));
    }

    @Test
    void counterIncrementMultipleTimes() {
        final Counter counter = observance.counter("errors");
        counter.increment();
        counter.increment();
        counter.increment();
        assertEquals(3, observance.counterValue("errors"));
    }

    // --- counter: increment(amount) ---

    @Test
    void counterIncrementByAmount() {
        observance.counter("bytes").increment(100);
        assertEquals(100, observance.counterValue("bytes"));
    }

    @Test
    void counterIncrementByAmountAccumulates() {
        final Counter counter = observance.counter("bytes");
        counter.increment(10);
        counter.increment(20);
        assertEquals(30, observance.counterValue("bytes"));
    }

    @Test
    void counterIncrementByZeroIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> observance.counter("x").increment(0));
    }

    @Test
    void counterIncrementByNegativeAmountIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> observance.counter("x").increment(-1));
    }

    // --- timer: null guard ---

    @Test
    void timerRejectsNullName() {
        assertThrows(NullPointerException.class, () -> observance.timer(null));
    }

    // --- timer: identity ---

    @Test
    void repeatedTimerCallsReturnSameLogicalTimer() {
        observance.timer("op").record(Duration.ofMillis(10));
        observance.timer("op").record(Duration.ofMillis(20));
        assertEquals(2, observance.timerCount("op"));
        assertEquals(Duration.ofMillis(30), observance.timerTotalDuration("op"));
    }

    // --- timer: initial state ---

    @Test
    void timerCountStartsAtZero() {
        assertEquals(0, observance.timerCount("new-timer"));
    }

    @Test
    void timerTotalDurationStartsAtZero() {
        assertEquals(Duration.ZERO, observance.timerTotalDuration("new-timer"));
    }

    // --- timer: record(Duration) ---

    @Test
    void timerRecordsDuration() {
        observance.timer("db").record(Duration.ofMillis(50));
        assertEquals(1, observance.timerCount("db"));
        assertEquals(Duration.ofMillis(50), observance.timerTotalDuration("db"));
    }

    @Test
    void timerRecordsDurationAccumulates() {
        final Timer timer = observance.timer("db");
        timer.record(Duration.ofMillis(10));
        timer.record(Duration.ofMillis(20));
        timer.record(Duration.ofMillis(30));
        assertEquals(3, observance.timerCount("db"));
        assertEquals(Duration.ofMillis(60), observance.timerTotalDuration("db"));
    }

    @Test
    void timerRecordDurationRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> observance.timer("db").record((Duration) null));
    }

    // --- timer: record(Runnable) ---

    @Test
    void timerRecordsRunnableExecution() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        observance.timer("task").record(() -> executed.set(true));

        assertTrue(executed.get(), "runnable must be executed");
        assertEquals(1, observance.timerCount("task"));
        assertTrue(observance.timerTotalDuration("task").toNanos() >= 0);
    }

    @Test
    void timerRecordRunnableRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> observance.timer("task").record((Runnable) null));
    }

    // --- timer: record(Supplier) ---

    @Test
    void timerRecordsSupplierExecutionAndReturnsResult() {
        final String result = observance.timer("fetch").record(() -> "hello");

        assertEquals("hello", result);
        assertEquals(1, observance.timerCount("fetch"));
        assertTrue(observance.timerTotalDuration("fetch").toNanos() >= 0);
    }

    @Test
    void timerRecordSupplierRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> observance.timer("fetch").record((java.util.function.Supplier<?>) null));
    }

    // --- read methods: null guard ---

    @Test
    void counterValueRejectsNullName() {
        assertThrows(NullPointerException.class, () -> observance.counterValue(null));
    }

    @Test
    void timerCountRejectsNullName() {
        assertThrows(NullPointerException.class, () -> observance.timerCount(null));
    }

    @Test
    void timerTotalDurationRejectsNullName() {
        assertThrows(NullPointerException.class, () -> observance.timerTotalDuration(null));
    }
}
