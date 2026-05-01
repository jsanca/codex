package codex.codex.internal.service;

import codex.codex.api.model.service.SiteService;
import codex.codex.internal.repository.MemorySiteRepository;
import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reusable test fixture that wires the full site event pipeline using real production
 * components — no mocks, no Spring, no external infrastructure.
 *
 * <p>Pipeline:</p>
 * <pre>
 * TransactionContext
 *   → EventPublishingSiteService
 *     → CodexSiteService
 *       → MemorySiteRepository
 *   → DeferredEventDispatcher
 *     → RecordingCodexEventDispatcher
 * </pre>
 */
public final class TestCodexContext {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-30T12:00:00Z"), ZoneId.of("UTC"));

    private final SiteService siteService;
    private final RecordingCodexEventDispatcher recordingDispatcher;

    private TestCodexContext(final SiteService siteService,
                             final RecordingCodexEventDispatcher recordingDispatcher) {
        this.siteService = Objects.requireNonNull(siteService);
        this.recordingDispatcher = Objects.requireNonNull(recordingDispatcher);
    }

    /**
     * Creates a fully wired context with a fresh in-memory repository.
     *
     * @return a new context ready for use in a single test
     */
    public static TestCodexContext create() {
        final MemorySiteRepository repository = new MemorySiteRepository();
        final RecordingCodexEventDispatcher recording = new RecordingCodexEventDispatcher();
        final DeferredEventDispatcher deferred = new DeferredEventDispatcher(recording, new SynchronousExecutor());
        final CodexSiteService core = new CodexSiteService(repository, CLOCK);
        final SiteService service = new EventPublishingSiteService(core, deferred, CLOCK);
        return new TestCodexContext(service, recording);
    }

    /**
     * Returns the wired {@link SiteService} entry point.
     *
     * @return the site service
     */
    public SiteService siteService() {
        return siteService;
    }

    /**
     * Returns all events dispatched so far.
     *
     * @return immutable snapshot of dispatched events
     */
    public List<CodexEvent> events() {
        return List.copyOf(recordingDispatcher.dispatched);
    }

    /**
     * Asserts that no events have been dispatched since the last {@link #clearEvents()}.
     */
    public void assertNoEvents() {
        assertTrue(recordingDispatcher.dispatched.isEmpty(),
                "Expected no events but found: " + recordingDispatcher.dispatched);
    }

    /**
     * Asserts that exactly one event of the given type has been dispatched and returns it.
     *
     * @param <E>  the event type
     * @param type the expected event class
     * @return the single dispatched event cast to {@code type}
     */
    public <E extends CodexEvent> E assertSingleEvent(final Class<E> type) {
        assertEquals(1, recordingDispatcher.dispatched.size(),
                "Expected exactly one event but found: " + recordingDispatcher.dispatched);
        assertInstanceOf(type, recordingDispatcher.dispatched.getFirst());
        return type.cast(recordingDispatcher.dispatched.getFirst());
    }

    /**
     * Clears all recorded events. Call between logical steps of a multi-step test.
     */
    public void clearEvents() {
        recordingDispatcher.dispatched.clear();
    }

    private static final class RecordingCodexEventDispatcher implements CodexEventDispatcher {

        final List<CodexEvent> dispatched = new ArrayList<>();

        @Override
        public void dispatch(final CodexEvent event) {
            dispatched.add(Objects.requireNonNull(event));
        }
    }

    private static final class SynchronousExecutor implements CodexExecutor {

        @Override
        public void submit(final Runnable task) {
            task.run();
        }

        @Override
        public void shutdown() {
        }
    }
}
