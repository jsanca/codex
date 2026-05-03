package codex.fundamentum.api.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LocalCodexEventDispatcher}.
 */
class LocalCodexEventDispatcherTest {

    // --- minimal test event records ---

    record AlphaEvent(Instant occurredAt) implements CodexEvent {}
    record BetaEvent(Instant occurredAt) implements CodexEvent {}

    // --- recording subscriber helper ---

    static final class RecordingSubscriber<E extends CodexEvent> implements CodexEventSubscriber<E> {
        private final Class<E> type;
        private final List<E> received = new ArrayList<>();

        RecordingSubscriber(final Class<E> type) {
            this.type = type;
        }

        @Override
        public Class<E> eventType() {
            return type;
        }

        @Override
        public void handle(final E event) {
            received.add(event);
        }

        List<E> received() {
            return List.copyOf(received);
        }
    }

    // --- 1: matching dispatch ---

    @Test
    void dispatchSendsMatchingEventToSubscriber() {
        final RecordingSubscriber<AlphaEvent> subscriber = new RecordingSubscriber<>(AlphaEvent.class);
        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of(subscriber));
        final AlphaEvent event = new AlphaEvent(Instant.now());

        dispatcher.dispatch(event);

        assertEquals(1, subscriber.received().size());
        assertSame(event, subscriber.received().getFirst());
    }

    // --- 2: non-matching subscriber ---

    @Test
    void dispatchDoesNotSendEventToNonMatchingSubscriber() {
        final RecordingSubscriber<BetaEvent> subscriber = new RecordingSubscriber<>(BetaEvent.class);
        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of(subscriber));

        dispatcher.dispatch(new AlphaEvent(Instant.now()));

        assertTrue(subscriber.received().isEmpty());
    }

    // --- 3: multiple matching subscribers ---

    @Test
    void dispatchSendsEventToMultipleMatchingSubscribers() {
        final RecordingSubscriber<AlphaEvent> first = new RecordingSubscriber<>(AlphaEvent.class);
        final RecordingSubscriber<AlphaEvent> second = new RecordingSubscriber<>(AlphaEvent.class);
        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of(first, second));
        final AlphaEvent event = new AlphaEvent(Instant.now());

        dispatcher.dispatch(event);

        assertEquals(1, first.received().size());
        assertEquals(1, second.received().size());
    }

    // --- 4: no matching subscribers is no-op ---

    @Test
    void dispatchWithNoMatchingSubscribersIsNoOp() {
        final RecordingSubscriber<BetaEvent> subscriber = new RecordingSubscriber<>(BetaEvent.class);
        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of(subscriber));

        assertDoesNotThrow(() -> dispatcher.dispatch(new AlphaEvent(Instant.now())));
        assertTrue(subscriber.received().isEmpty());
    }

    // --- 5: constructor rejects null collection ---

    @Test
    void constructorRejectsNullSubscribersCollection() {
        assertThrows(NullPointerException.class, () -> new LocalCodexEventDispatcher(null));
    }

    // --- 6: constructor rejects null subscriber entry ---

    @Test
    void constructorRejectsNullSubscriberEntry() {
        final List<CodexEventSubscriber<?>> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new LocalCodexEventDispatcher(withNull));
    }

    // --- 7: dispatch rejects null event ---

    @Test
    void dispatchRejectsNullEvent() {
        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of());
        assertThrows(NullPointerException.class, () -> dispatcher.dispatch(null));
    }

    // --- 8: subscriber exception propagates ---

    @Test
    void subscriberExceptionPropagates() {
        final CodexEventSubscriber<AlphaEvent> throwing = new CodexEventSubscriber<>() {
            @Override
            public Class<AlphaEvent> eventType() {
                return AlphaEvent.class;
            }

            @Override
            public void handle(final AlphaEvent event) {
                throw new RuntimeException("subscriber failure");
            }
        };
        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of(throwing));

        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dispatcher.dispatch(new AlphaEvent(Instant.now())));
        assertEquals("subscriber failure", ex.getMessage());
    }

    // --- 9: empty subscriber list ---

    @Test
    void dispatchWithEmptySubscriberListIsNoOp() {
        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of());
        assertDoesNotThrow(() -> dispatcher.dispatch(new AlphaEvent(Instant.now())));
    }
}
