package codex.fundamentum.api.event;

import codex.fundamentum.api.observance.InMemoryObservance;
import codex.fundamentum.api.observance.Observance;
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

    // --- 10: default constructor uses noop observance (no metrics, no exception) ---

    @Test
    void defaultConstructorPathWorksWithoutObservance() {
        final RecordingSubscriber<AlphaEvent> subscriber = new RecordingSubscriber<>(AlphaEvent.class);
        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of(subscriber));
        assertDoesNotThrow(() -> dispatcher.dispatch(new AlphaEvent(Instant.now())));
        assertEquals(1, subscriber.received().size());
    }

    @Test
    void constructorRejectsNullObservance() {
        assertThrows(NullPointerException.class,
                () -> new LocalCodexEventDispatcher(List.of(), null));
    }

    // --- 11: observance — event dispatch counter ---

    @Test
    void dispatchingEventIncrementsEventsDispatchedCounter() {
        final InMemoryObservance observance = new InMemoryObservance();
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(), observance);

        dispatcher.dispatch(new AlphaEvent(Instant.now()));

        assertEquals(1, observance.counterValue("events.dispatched.AlphaEvent"));
    }

    @Test
    void dispatchingEventTwiceIncrementsCounterTwice() {
        final InMemoryObservance observance = new InMemoryObservance();
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(), observance);

        dispatcher.dispatch(new AlphaEvent(Instant.now()));
        dispatcher.dispatch(new AlphaEvent(Instant.now()));

        assertEquals(2, observance.counterValue("events.dispatched.AlphaEvent"));
    }

    @Test
    void differentEventTypesUseDistinctCounters() {
        final InMemoryObservance observance = new InMemoryObservance();
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(), observance);

        dispatcher.dispatch(new AlphaEvent(Instant.now()));
        dispatcher.dispatch(new BetaEvent(Instant.now()));

        assertEquals(1, observance.counterValue("events.dispatched.AlphaEvent"));
        assertEquals(1, observance.counterValue("events.dispatched.BetaEvent"));
    }

    // --- 12: observance — subscriber invocation counter ---

    @Test
    void matchingSubscriberInvocationIncrementsSubscribersInvokedCounter() {
        final InMemoryObservance observance = new InMemoryObservance();
        final RecordingSubscriber<AlphaEvent> subscriber = new RecordingSubscriber<>(AlphaEvent.class);
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(subscriber), observance);

        dispatcher.dispatch(new AlphaEvent(Instant.now()));

        assertEquals(1, observance.counterValue("subscribers.invoked.RecordingSubscriber"));
    }

    @Test
    void nonMatchingSubscriberDoesNotIncrementInvokedCounter() {
        final InMemoryObservance observance = new InMemoryObservance();
        final RecordingSubscriber<BetaEvent> subscriber = new RecordingSubscriber<>(BetaEvent.class);
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(subscriber), observance);

        dispatcher.dispatch(new AlphaEvent(Instant.now()));

        assertEquals(0, observance.counterValue("subscribers.invoked.RecordingSubscriber"));
    }

    // --- 13: observance — subscriber duration timer ---

    @Test
    void subscriberDurationCountIncrements() {
        final InMemoryObservance observance = new InMemoryObservance();
        final RecordingSubscriber<AlphaEvent> subscriber = new RecordingSubscriber<>(AlphaEvent.class);
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(subscriber), observance);

        dispatcher.dispatch(new AlphaEvent(Instant.now()));

        assertEquals(1, observance.timerCount("subscribers.duration.RecordingSubscriber"));
    }

    // --- 14: observance — subscriber failure counter ---

    static final class FailingSubscriber implements CodexEventSubscriber<AlphaEvent> {
        @Override
        public Class<AlphaEvent> eventType() { return AlphaEvent.class; }

        @Override
        public void handle(final AlphaEvent event) {
            throw new RuntimeException("intentional failure");
        }
    }

    @Test
    void failingSubscriberIncrementsSubscribersFailedCounter() {
        final InMemoryObservance observance = new InMemoryObservance();
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(new FailingSubscriber()), observance);

        assertThrows(RuntimeException.class,
                () -> dispatcher.dispatch(new AlphaEvent(Instant.now())));

        assertEquals(1, observance.counterValue("subscribers.failed.FailingSubscriber"));
    }

    @Test
    void failingSubscriberStillRecordsDuration() {
        final InMemoryObservance observance = new InMemoryObservance();
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(new FailingSubscriber()), observance);

        assertThrows(RuntimeException.class,
                () -> dispatcher.dispatch(new AlphaEvent(Instant.now())));

        assertEquals(1, observance.timerCount("subscribers.duration.FailingSubscriber"));
    }

    @Test
    void failingSubscriberExceptionStillPropagates() {
        final InMemoryObservance observance = new InMemoryObservance();
        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(List.of(new FailingSubscriber()), observance);

        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dispatcher.dispatch(new AlphaEvent(Instant.now())));
        assertEquals("intentional failure", ex.getMessage());
    }
}
