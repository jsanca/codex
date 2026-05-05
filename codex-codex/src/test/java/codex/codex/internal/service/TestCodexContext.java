package codex.codex.internal.service;

import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.service.SiteService;
import codex.codex.api.runtime.CodexRuntime;
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
     * Uses a {@link codex.codex.internal.index.NoOpIndexWriter} by default.
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
     * Returns the wired {@link ContentTypeService} entry point.
     *
     * @return the content type service
     */
    public ContentTypeService contentTypeService() {
        return runtime.contentTypeService();
    }

    /**
     * Returns the wired {@link ContentItemService} entry point.
     *
     * @return the content item service
     */
    public ContentItemService contentItemService() {
        return runtime.contentItemService();
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

    /**
     * Returns all recorded events of the given type.
     *
     * @param <E>  the event type
     * @param type the event class to filter by
     * @return immutable list of matching events; never null
     */
    public <E extends CodexEvent> List<E> eventsOfType(final Class<E> type) {
        return runtime.recordedEvents().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    /**
     * Asserts that exactly one event of the given type has been dispatched and returns it.
     * Unlike {@link #assertSingleEvent(Class)}, this does not require the event to be the only
     * recorded event — it only checks that exactly one event of the specified type exists.
     *
     * @param <E>  the event type
     * @param type the expected event class
     * @return the single dispatched event of {@code type}
     */
    public <E extends CodexEvent> E assertSingleEventOfType(final Class<E> type) {
        final List<E> found = eventsOfType(type);
        assertEquals(1, found.size(),
                "Expected exactly one " + type.getSimpleName() + " but found " + found.size()
                        + " in: " + runtime.recordedEvents());
        return found.getFirst();
    }

    /**
     * Asserts that no event of the given type has been dispatched.
     *
     * @param <E>  the event type
     * @param type the event class to check for
     */
    public <E extends CodexEvent> void assertNoEventsOfType(final Class<E> type) {
        final List<E> found = eventsOfType(type);
        assertTrue(found.isEmpty(),
                "Expected no " + type.getSimpleName() + " but found: " + found);
    }
}
