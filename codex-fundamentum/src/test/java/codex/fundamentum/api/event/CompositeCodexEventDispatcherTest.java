package codex.fundamentum.api.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CompositeCodexEventDispatcher}.
 */
class CompositeCodexEventDispatcherTest {

    record TestEvent(Instant occurredAt) implements CodexEvent {}

    static final class RecordingDispatcher implements CodexEventDispatcher {
        private final List<CodexEvent> received = new ArrayList<>();

        @Override
        public void dispatch(final CodexEvent event) {
            received.add(event);
        }

        List<CodexEvent> received() {
            return List.copyOf(received);
        }
    }

    // --- 1: dispatch to all delegates ---

    @Test
    void dispatchSendsEventToAllDelegateDispatchers() {
        final RecordingDispatcher first = new RecordingDispatcher();
        final RecordingDispatcher second = new RecordingDispatcher();
        final CompositeCodexEventDispatcher composite =
                new CompositeCodexEventDispatcher(List.of(first, second));
        final TestEvent event = new TestEvent(Instant.now());

        composite.dispatch(event);

        assertEquals(1, first.received().size());
        assertEquals(1, second.received().size());
        assertSame(event, first.received().getFirst());
        assertSame(event, second.received().getFirst());
    }

    // --- 2: empty delegates is no-op ---

    @Test
    void dispatchWithEmptyDelegatesIsNoOp() {
        final CompositeCodexEventDispatcher composite = new CompositeCodexEventDispatcher(List.of());
        assertDoesNotThrow(() -> composite.dispatch(new TestEvent(Instant.now())));
    }

    // --- 3: constructor rejects null collection ---

    @Test
    void constructorRejectsNullDispatchersCollection() {
        assertThrows(NullPointerException.class, () -> new CompositeCodexEventDispatcher(null));
    }

    // --- 4: constructor rejects null dispatcher entry ---

    @Test
    void constructorRejectsNullDispatcherEntry() {
        final List<CodexEventDispatcher> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new CompositeCodexEventDispatcher(withNull));
    }

    // --- 5: dispatch rejects null event ---

    @Test
    void dispatchRejectsNullEvent() {
        final CompositeCodexEventDispatcher composite = new CompositeCodexEventDispatcher(List.of());
        assertThrows(NullPointerException.class, () -> composite.dispatch(null));
    }

    // --- 6: exception from delegate propagates ---

    @Test
    void exceptionFromDelegatePropagates() {
        final CodexEventDispatcher throwing = event -> {
            throw new RuntimeException("delegate failure");
        };
        final CompositeCodexEventDispatcher composite =
                new CompositeCodexEventDispatcher(List.of(throwing));

        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> composite.dispatch(new TestEvent(Instant.now())));
        assertEquals("delegate failure", ex.getMessage());
    }

    // --- 7: delegates after throwing delegate are not called ---

    @Test
    void delegatesAfterThrowingDelegateAreNotCalled() {
        final CodexEventDispatcher throwing = event -> {
            throw new RuntimeException("delegate failure");
        };
        final RecordingDispatcher after = new RecordingDispatcher();
        final CompositeCodexEventDispatcher composite =
                new CompositeCodexEventDispatcher(List.of(throwing, after));

        assertThrows(RuntimeException.class,
                () -> composite.dispatch(new TestEvent(Instant.now())));

        assertTrue(after.received().isEmpty());
    }
}
