package codex.fundamentum.api.event;

/**
 * A typed in-process subscriber for a specific {@link CodexEvent} type.
 * <p>
 * Subscribers are explicit and interface-based. There are no annotations, no reflection scanning,
 * and no ordering guarantees. {@link LocalCodexEventDispatcher} uses {@link #eventType()} to
 * route events only to subscribers whose type matches the dispatched event.
 * <p>
 * Subscribers are extension points for:
 * <ul>
 *   <li>search indexing projections</li>
 *   <li>cache invalidation</li>
 *   <li>audit</li>
 *   <li>workflow continuation</li>
 *   <li>external broker publishing</li>
 * </ul>
 * Annotation-based subscriber discovery is future work.
 *
 * @param <E> the event type this subscriber handles; must extend {@link CodexEvent}
 */
public interface CodexEventSubscriber<E extends CodexEvent> {

    /**
     * Returns the event type this subscriber handles.
     * <p>
     * Used by {@link LocalCodexEventDispatcher} to match incoming events to registered subscribers.
     *
     * @return the handled event class; must not be null
     */
    Class<E> eventType();

    /**
     * Handles the given event.
     * <p>
     * Implementations should be fast and synchronous. No async execution,
     * no retry, and no checked exceptions.
     *
     * @param event the event to handle; must not be null
     */
    void handle(E event);
}
