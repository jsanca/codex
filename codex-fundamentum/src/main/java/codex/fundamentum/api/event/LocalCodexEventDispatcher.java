package codex.fundamentum.api.event;

import codex.fundamentum.api.observance.Observance;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * An in-process {@link CodexEventDispatcher} that delivers events synchronously to registered
 * {@link CodexEventSubscriber} instances.
 * <p>
 * Routing is type-safe: only subscribers whose {@link CodexEventSubscriber#eventType()} matches
 * the dispatched event (via {@link Class#isInstance(Object)}) receive the call. If no subscriber
 * matches, dispatch is a no-op.
 * <p>
 * Events are dispatched in the calling thread, in collection-iteration order. There is no async
 * execution, no retry, and no dead-letter behavior. If a subscriber throws, the exception propagates
 * immediately. Production-grade error handling can be layered on top later.
 * <p>
 * Typical position in the Codex pipeline:
 * <pre>
 * DeferredEventDispatcher (transaction-aware)
 *   → LocalCodexEventDispatcher (in-process routing)
 *      → CodexEventSubscriber&lt;ContentItemPublishedEvent&gt;
 *         → ContentItemPublishedIndexingSubscriber → IndexWriter
 * </pre>
 * Codex now has an explicit local subscriber contract. Subscribers are in-process handlers.
 * {@link codex.fundamentum.api.tx.TransactionContext}-aware delivery remains the responsibility
 * of {@code DeferredEventDispatcher}. Annotation-based subscriber discovery is future work.
 *
 * <p>Metrics emitted per dispatch (via the provided {@link Observance}):
 * <ul>
 *   <li>{@code events.dispatched.{EventSimpleName}} — incremented once per {@link #dispatch} call</li>
 *   <li>{@code subscribers.invoked.{SubscriberSimpleName}} — incremented per matching subscriber</li>
 *   <li>{@code subscribers.duration.{SubscriberSimpleName}} — timed per matching subscriber (success and failure)</li>
 *   <li>{@code subscribers.failed.{SubscriberSimpleName}} — incremented when a subscriber throws</li>
 * </ul>
 *
 * Note:
 * <pre>
 *     Subscribers are fixed at dispatcher construction time.
 *     Dynamic subscriber registration is future work.
 * </pre>
 */
public final class LocalCodexEventDispatcher implements CodexEventDispatcher {

    private final List<CodexEventSubscriber<? extends CodexEvent>> subscribers;
    private final Observance observance;

    /**
     * Creates a new {@code LocalCodexEventDispatcher} with the given subscribers and no-op observance.
     *
     * @param subscribers the subscribers to route events to; must not be null; must not contain null entries
     * @throws NullPointerException if {@code subscribers} is null or contains a null entry
     */
    public LocalCodexEventDispatcher(
            final Collection<? extends CodexEventSubscriber<? extends CodexEvent>> subscribers) {
        this(subscribers, Observance.noop());
    }

    /**
     * Creates a new {@code LocalCodexEventDispatcher} with the given subscribers and observance.
     *
     * @param subscribers the subscribers to route events to; must not be null; must not contain null entries
     * @param observance  the observance for metrics collection; must not be null
     * @throws NullPointerException if {@code subscribers}, any entry, or {@code observance} is null
     */
    public LocalCodexEventDispatcher(
            final Collection<? extends CodexEventSubscriber<? extends CodexEvent>> subscribers,
            final Observance observance) {
        Objects.requireNonNull(subscribers, "subscribers must not be null");
        Objects.requireNonNull(observance, "observance must not be null");
        for (final var subscriber : subscribers) {
            Objects.requireNonNull(subscriber, "subscribers must not contain null entries");
            Objects.requireNonNull(subscriber.eventType(), "subscriber eventType must not be null");
        }
        this.subscribers = List.copyOf(subscribers);
        this.observance = observance;
    }

    /**
     * Dispatches the event to all subscribers whose {@link CodexEventSubscriber#eventType()} matches.
     *
     * @param event the event to dispatch; must not be null
     * @throws NullPointerException if {@code event} is null
     */
    @Override
    public void dispatch(final CodexEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        observance.counter(EventDispatchMetricNames.eventsDispatched(event.getClass().getSimpleName())).increment();
        for (final CodexEventSubscriber<? extends CodexEvent> subscriber : subscribers) {
            if (subscriber.eventType().isInstance(event)) {
                dispatchToSubscriber(subscriber, event);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends CodexEvent> void dispatchToSubscriber(
            final CodexEventSubscriber<E> subscriber, final CodexEvent event) {
        final String name = subscriberName(subscriber);
        observance.counter(EventDispatchMetricNames.subscribersInvoked(name)).increment();
        try {
            observance.timer(EventDispatchMetricNames.subscribersDuration(name)).record(() -> subscriber.handle((E) event));
        } catch (final RuntimeException ex) {
            observance.counter(EventDispatchMetricNames.subscribersFailed(name)).increment();
            throw ex;
        }
    }

    private static String subscriberName(final CodexEventSubscriber<?> subscriber) {
        final String name = subscriber.getClass().getSimpleName();
        return name.isBlank() ? "anonymous" : name;
    }
}
