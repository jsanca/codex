package codex.fundamentum.api.event;

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
 * Note:
 * <pre>
 *     Subscribers are fixed at dispatcher construction time.
 *     Dynamic subscriber registration is future work.
 * </pre>
 */
public final class LocalCodexEventDispatcher implements CodexEventDispatcher {

    private final List<CodexEventSubscriber<? extends CodexEvent>> subscribers;

    /**
     * Creates a new {@code LocalCodexEventDispatcher} with the given subscribers.
     *
     * @param subscribers the subscribers to route events to; must not be null; must not contain null entries
     * @throws NullPointerException if {@code subscribers} is null or contains a null entry
     */
    public LocalCodexEventDispatcher(
            final Collection<? extends CodexEventSubscriber<? extends CodexEvent>> subscribers) {
        Objects.requireNonNull(subscribers, "subscribers must not be null");
        for (final var subscriber : subscribers) {
            Objects.requireNonNull(subscriber, "subscribers must not contain null entries");
            Objects.requireNonNull(subscriber.eventType(), "subscriber eventType must not be null");
        }
        this.subscribers = List.copyOf(subscribers);
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
        for (final CodexEventSubscriber<? extends CodexEvent> subscriber : subscribers) {
            if (subscriber.eventType().isInstance(event)) {
                dispatchToSubscriber(subscriber, event);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends CodexEvent> void dispatchToSubscriber(
            final CodexEventSubscriber<E> subscriber, final CodexEvent event) {
        subscriber.handle((E) event);
    }
}
