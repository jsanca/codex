package codex.concilium.internal;

import codex.chronicon.api.runtime.ChroniconRuntime;
import codex.codex.api.runtime.CodexRuntime;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.runtime.CodexModuleRuntime;
import codex.index.api.runtime.IndexRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Local runtime council for Codex.
 *
 * <p>Composes the canonical core runtime, the indexing projection runtime, and the
 * audit/history projection runtime into a single coherent application runtime. All
 * module subscribers share a unified event dispatcher so that domain events emitted by
 * core services are automatically delivered to Index and Chronicon.</p>
 *
 * <p>Usage (in-memory, suitable for tests and early development):</p>
 * <pre>{@code
 * ConciliumRuntime runtime = ConciliumRuntime.inMemory();
 * runtime.coreRuntime().siteService().create(..., actor);
 * // SiteCreatedEvent reaches ChroniconRuntime subscribers automatically
 * }</pre>
 *
 * <p>Usage with custom child runtimes (e.g. recording helpers in integration tests):</p>
 * <pre>{@code
 * AtomicReference<CodexEventDispatcher> placeholder = new AtomicReference<>(event -> {});
 * CodexRuntime core = CodexRuntime.inMemory(event -> placeholder.get().dispatch(event));
 * IndexRuntime index = IndexRuntime.withWriter(core.contentItemProjectionReader(), recordingWriter);
 * ChroniconRuntime chronicon = ChroniconRuntime.withRepository(recordingRepo);
 * List<...> subs = new ArrayList<>(index.subscribers());
 * subs.addAll(chronicon.subscribers());
 * placeholder.set(new LocalCodexEventDispatcher(subs));
 * ConciliumRuntime runtime = ConciliumRuntime.compose(core, index, chronicon);
 * }</pre>
 *
 * <p>No ServiceLoader, Spring, global registry, or dynamic subscriber discovery is used.</p>
 */
public final class ConciliumRuntime implements CodexModuleRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConciliumRuntime.class);
    private static final String MODULE_NAME = "codex-concilium";

    private final CodexRuntime coreRuntime;
    private final IndexRuntime indexRuntime;
    private final ChroniconRuntime chroniconRuntime;
    private final List<CodexEventSubscriber<? extends CodexEvent>> subscribers;
    private final CodexEventDispatcher eventDispatcher;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private ConciliumRuntime(
            final CodexRuntime coreRuntime,
            final IndexRuntime indexRuntime,
            final ChroniconRuntime chroniconRuntime,
            final List<CodexEventSubscriber<? extends CodexEvent>> subscribers,
            final CodexEventDispatcher eventDispatcher) {
        this.coreRuntime = coreRuntime;
        this.indexRuntime = indexRuntime;
        this.chroniconRuntime = chroniconRuntime;
        this.subscribers = subscribers;
        this.eventDispatcher = eventDispatcher;
    }

    // --- factories ---

    /**
     * Creates a fully wired in-memory runtime composing all three module runtimes.
     *
     * <p>Uses a forwarding lambda to break the circular dependency between
     * {@code CodexRuntime} creation (which needs the module dispatcher) and
     * {@code IndexRuntime} creation (which needs the projection reader from
     * {@code CodexRuntime}). The forwarding reference is set once before any
     * service call can occur and is effectively fixed for the lifetime of the runtime.</p>
     *
     * <p>The no-op index writer from {@link IndexRuntime#inMemory} is used by default.
     * Use {@link #compose} with a {@link codex.index.internal.RecordingIndexWriter} for tests
     * that need to inspect index writes.</p>
     *
     * @return a new, fully assembled {@code ConciliumRuntime}
     */
    public static ConciliumRuntime inMemory() {
        LOGGER.info("Assembling ConciliumRuntime (in-memory)");

        // A forwarding dispatcher breaks the circular dependency:
        // CodexRuntime needs the module dispatcher at construction time, but IndexRuntime
        // needs core.contentItemProjectionReader() — which requires CodexRuntime to exist first.
        final AtomicReference<CodexEventDispatcher> placeholder =
                new AtomicReference<>(event -> {});
        final CodexRuntime core = CodexRuntime.inMemory(
                event -> placeholder.get().dispatch(event));

        final IndexRuntime index = IndexRuntime.inMemory(core.contentItemProjectionReader());
        final ChroniconRuntime chronicon = ChroniconRuntime.inMemory();

        final List<CodexEventSubscriber<? extends CodexEvent>> allSubscribers =
                buildSubscriberList(index, chronicon);
        final LocalCodexEventDispatcher moduleDispatcher =
                new LocalCodexEventDispatcher(allSubscribers);
        placeholder.set(moduleDispatcher);

        LOGGER.info("ConciliumRuntime ready: {} subscribers wired", allSubscribers.size());
        return new ConciliumRuntime(core, index, chronicon, allSubscribers, moduleDispatcher);
    }

    /**
     * Composes pre-built module runtimes into a {@code ConciliumRuntime}.
     *
     * <p>The {@code coreRuntime} must have been created with
     * {@link CodexRuntime#inMemory(CodexEventDispatcher)} and a dispatcher that routes
     * events to the subscribers of the provided {@code indexRuntime} and
     * {@code chroniconRuntime}. See the class-level Javadoc for the recommended pattern.</p>
     *
     * @param coreRuntime       the canonical core runtime; must not be null
     * @param indexRuntime      the indexing projection runtime; must not be null
     * @param chroniconRuntime  the audit/history projection runtime; must not be null
     * @return a new, assembled {@code ConciliumRuntime}
     */
    public static ConciliumRuntime compose(
            final CodexRuntime coreRuntime,
            final IndexRuntime indexRuntime,
            final ChroniconRuntime chroniconRuntime) {
        Objects.requireNonNull(coreRuntime, "coreRuntime must not be null");
        Objects.requireNonNull(indexRuntime, "indexRuntime must not be null");
        Objects.requireNonNull(chroniconRuntime, "chroniconRuntime must not be null");

        final List<CodexEventSubscriber<? extends CodexEvent>> allSubscribers =
                buildSubscriberList(indexRuntime, chroniconRuntime);
        final LocalCodexEventDispatcher moduleDispatcher =
                new LocalCodexEventDispatcher(allSubscribers);

        return new ConciliumRuntime(coreRuntime, indexRuntime, chroniconRuntime,
                allSubscribers, moduleDispatcher);
    }

    // --- CodexModuleRuntime ---

    /**
     * {@inheritDoc}
     *
     * @return {@code "codex-concilium"}
     */
    @Override
    public String moduleName() {
        return MODULE_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the combined subscriber list from {@link IndexRuntime} and
     * {@link ChroniconRuntime}. Does not include core-internal observers such as
     * {@code EventRecorder}.</p>
     *
     * @return immutable list containing all module subscribers
     */
    @Override
    public List<CodexEventSubscriber<? extends CodexEvent>> subscribers() {
        return subscribers;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Closes child runtimes in reverse composition order: Chronicon → Index → Core.
     * Idempotent — safe to call multiple times.</p>
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            LOGGER.info("Shutting down ConciliumRuntime");
            chroniconRuntime.close();
            indexRuntime.close();
            coreRuntime.close();
        }
    }

    // --- accessors ---

    /**
     * Returns the canonical core runtime.
     *
     * @return the core runtime; never null
     */
    public CodexRuntime coreRuntime() {
        return coreRuntime;
    }

    /**
     * Returns the indexing projection runtime.
     *
     * @return the index runtime; never null
     */
    public IndexRuntime indexRuntime() {
        return indexRuntime;
    }

    /**
     * Returns the audit/history projection runtime.
     *
     * @return the chronicon runtime; never null
     */
    public ChroniconRuntime chroniconRuntime() {
        return chroniconRuntime;
    }

    /**
     * Returns the module-side event dispatcher used by this runtime.
     *
     * <p>This is the {@link LocalCodexEventDispatcher} that holds all subscribers from
     * {@link IndexRuntime} and {@link ChroniconRuntime}. Events dispatched here are delivered
     * directly to module subscribers without going through the core's deferred pipeline.</p>
     *
     * @return the module event dispatcher; never null
     */
    public CodexEventDispatcher eventDispatcher() {
        return eventDispatcher;
    }

    // --- private helpers ---

    private static List<CodexEventSubscriber<? extends CodexEvent>> buildSubscriberList(
            final IndexRuntime indexRuntime,
            final ChroniconRuntime chroniconRuntime) {
        final List<CodexEventSubscriber<? extends CodexEvent>> all = new ArrayList<>();
        all.addAll(indexRuntime.subscribers());
        all.addAll(chroniconRuntime.subscribers());
        return List.copyOf(all);
    }
}
