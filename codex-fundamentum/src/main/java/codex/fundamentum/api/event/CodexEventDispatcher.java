package codex.fundamentum.api.event;


import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches Codex events to interested subscribers.
 */
public interface CodexEventDispatcher {

    /**
     * Dispatches the given event.
     *
     * @param event The event to dispatch. Must not be null.
     */
    void dispatch(CodexEvent event);

    /**
     * Returns a dispatcher that ignores every event.
     *
     * <p>This is useful as a default implementation while the event system is
     * being designed, or when a module does not need event handling yet.</p>
     *
     * @return A no-operation event dispatcher.
     */
    static CodexEventDispatcher noop() {
        return NoOpCodexEventDispatcher.INSTANCE;
    }

    /**
     * Returns an in-memory dispatcher that records dispatched events.
     *
     * <p>This is mainly useful for tests and early development because callers
     * can inspect which events were emitted.</p>
     *
     * @return A recording event dispatcher.
     */
    static CodexEventDispatcher recording() {
        return new LoggerCodexEventDispatcher();
    }
}

final class NoOpCodexEventDispatcher implements CodexEventDispatcher {

    static final NoOpCodexEventDispatcher INSTANCE = new NoOpCodexEventDispatcher();

    private NoOpCodexEventDispatcher() {
    }

    @Override
    public void dispatch(final CodexEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
    }
}

final class LoggerCodexEventDispatcher implements CodexEventDispatcher {

    private final Set<CodexEvent> events = ConcurrentHashMap.newKeySet();

    @Override
    public void dispatch(final CodexEvent event) {
        events.add(Objects.requireNonNull(event, "event cannot be null"));
    }

    public List<CodexEvent> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }

    @Override
    public String toString() {
        return events.toString();
    }
}
