package codex.codex.internal.service;

import codex.codex.api.model.service.SiteService;
import codex.codex.internal.runtime.CodexRuntime;
import codex.fundamentum.api.event.CodexEvent;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reusable test fixture that wires the full site event pipeline via {@link CodexRuntime}.
 * Each test should create a fresh instance via {@link #create()} to guarantee isolation.
 */
public final class TestCodexContext {

    private final CodexRuntime runtime;

    private TestCodexContext(final CodexRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime);
    }

    /**
     * Creates a fully wired in-memory context backed by {@link CodexRuntime#inMemory()}.
     *
     * @return a new, isolated context
     */
    public static TestCodexContext create() {
        return new TestCodexContext(CodexRuntime.inMemory());
    }

    /**
     * Returns the wired {@link SiteService} entry point.
     *
     * @return the site service
     */
    public SiteService siteService() {
        return runtime.siteService();
    }

    /**
     * Returns all events dispatched so far.
     *
     * @return immutable snapshot of dispatched events
     */
    public List<CodexEvent> events() {
        return runtime.recordedEvents();
    }

    /**
     * Asserts that no events have been dispatched since the last {@link #clearEvents()}.
     */
    public void assertNoEvents() {
        assertTrue(runtime.recordedEvents().isEmpty(),
                "Expected no events but found: " + runtime.recordedEvents());
    }

    /**
     * Asserts that exactly one event of the given type has been dispatched and returns it.
     *
     * @param <E>  the event type
     * @param type the expected event class
     * @return the single dispatched event cast to {@code type}
     */
    public <E extends CodexEvent> E assertSingleEvent(final Class<E> type) {
        final List<CodexEvent> events = runtime.recordedEvents();
        assertEquals(1, events.size(),
                "Expected exactly one event but found: " + events);
        assertInstanceOf(type, events.getFirst());
        return type.cast(events.getFirst());
    }

    /**
     * Clears all recorded events. Call between logical steps of a multi-step test.
     */
    public void clearEvents() {
        runtime.clearRecordedEvents();
    }
}
