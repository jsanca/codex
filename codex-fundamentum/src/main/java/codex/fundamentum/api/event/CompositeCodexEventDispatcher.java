package codex.fundamentum.api.event;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A fan-out {@link CodexEventDispatcher} that delivers events to multiple delegate dispatchers
 * in collection-iteration order.
 * <p>
 * This preserves the existing {@link CodexEventDispatcher} abstraction while allowing multiple
 * targets — recording, local subscribers, external broker publishing, audit, or other concerns —
 * to receive the same event without any caller knowing about the fan-out.
 * <p>
 * Dispatch is synchronous and sequential. If a delegate throws, the exception propagates
 * immediately. Remaining delegates are not called. No retry, no exception swallowing.
 * Transaction-aware delivery remains the responsibility of {@link codex.fundamentum.api.tx.TransactionContext}
 * and {@code DeferredEventDispatcher}.
 * <p>
 * Typical runtime pipeline:
 * <pre>
 * DeferredEventDispatcher (transaction-aware)
 *   → CompositeCodexEventDispatcher
 *      → EventRecorder (domain event recording)
 *      → LocalCodexEventDispatcher (in-process subscribers)
 *         → ContentItemPublishedIndexingSubscriber → IndexWriter
 * </pre>
 * The recording dispatcher is placed first so events are captured even if a subscriber fails.
 * Dynamic subscriber registration is future work.
 */
public final class CompositeCodexEventDispatcher implements CodexEventDispatcher {

    private final List<CodexEventDispatcher> dispatchers;

    /**
     * Creates a new {@code CompositeCodexEventDispatcher} with the given delegates.
     *
     * @param dispatchers the dispatchers to fan out to; must not be null; must not contain null entries
     * @throws NullPointerException if {@code dispatchers} is null or contains a null entry
     */
    public CompositeCodexEventDispatcher(final Collection<? extends CodexEventDispatcher> dispatchers) {
        Objects.requireNonNull(dispatchers, "dispatchers must not be null");
        for (final var dispatcher : dispatchers) {
            Objects.requireNonNull(dispatcher, "dispatchers must not contain null entries");
        }
        this.dispatchers = List.copyOf(dispatchers);
    }

    /**
     * Dispatches the event to every delegate dispatcher in collection-iteration order.
     * If a dispatcher throws, the exception propagates immediately.
     *
     * @param event the event to dispatch; must not be null
     * @throws NullPointerException if {@code event} is null
     */
    @Override
    public void dispatch(final CodexEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        for (final CodexEventDispatcher dispatcher : dispatchers) {
            dispatcher.dispatch(event);
        }
    }
}
