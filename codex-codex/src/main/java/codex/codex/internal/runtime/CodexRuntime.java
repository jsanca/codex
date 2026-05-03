package codex.codex.internal.runtime;

import codex.codex.api.index.IndexWriter;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.service.SiteService;
import codex.codex.internal.index.ContentItemIndexDocumentMapper;
import codex.codex.internal.index.ContentItemPublishedIndexingSubscriber;
import codex.codex.internal.index.NoOpIndexWriter;
import codex.codex.internal.repository.MemoryContentItemRepository;
import codex.codex.internal.repository.MemoryContentRevisionRepository;
import codex.codex.internal.repository.MemoryContentTypeRepository;
import codex.codex.internal.repository.MemoryContentTypeVersionRepository;
import codex.codex.internal.repository.MemorySiteRepository;
import codex.codex.internal.service.CodexContentItemService;
import codex.codex.internal.service.CodexContentTypeService;
import codex.codex.internal.service.CodexSiteService;
import codex.codex.internal.service.DeferredEventDispatcher;
import codex.codex.internal.service.EventPublishingContentItemService;
import codex.codex.internal.service.EventPublishingContentTypeService;
import codex.codex.internal.service.EventPublishingSiteService;
import codex.codex.internal.service.SiteIdentityGenerator;
import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.concurrent.CodexExecutorConfig;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.event.CompositeCodexEventDispatcher;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;

/**
 * Manual composition root for {@code codex-codex}.
 *
 * <p>Wires all real components together and exposes them through a clean API.
 * Callers never see the internal pipeline assembly.</p>
 *
 * <p>Obtain an instance via {@link #inMemory()} and release resources via
 * {@link #shutdown()} or try-with-resources.</p>
 *
 * <p>Event pipeline (in-memory):</p>
 * <pre>
 * DeferredEventDispatcher (transaction-aware)
 *   → CompositeCodexEventDispatcher
 *      → EventRecorder       (domain event recording, first so events survive subscriber failures)
 *      → LocalCodexEventDispatcher
 *         → ContentItemPublishedIndexingSubscriber → IndexWriter
 * </pre>
 *
 * <p>By default {@link NoOpIndexWriter} is used. Pass a custom {@link IndexWriter} via
 * {@link #inMemory(IndexWriter)} to enable recording or production indexing.</p>
 *
 * @author jsanca &amp; clio
 */
public final class CodexRuntime implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodexRuntime.class);

    private final SiteService siteService;
    private final ContentTypeService contentTypeService;
    private final ContentItemService contentItemService;
    private final CodexEventDispatcher eventDispatcher;
    private final EventRecorder eventRecorder;
    private final CodexExecutor asyncExecutor;
    private final Clock clock;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private CodexRuntime(final SiteService siteService,
                         final ContentTypeService contentTypeService,
                         final ContentItemService contentItemService,
                         final CodexEventDispatcher eventDispatcher,
                         final EventRecorder eventRecorder,
                         final CodexExecutor asyncExecutor,
                         final Clock clock) {
        this.siteService = Objects.requireNonNull(siteService);
        this.contentTypeService = Objects.requireNonNull(contentTypeService);
        this.contentItemService = Objects.requireNonNull(contentItemService);
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher);
        this.eventRecorder = Objects.requireNonNull(eventRecorder);
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Creates a fully wired in-memory runtime with a {@link NoOpIndexWriter}.
     * Indexing is structurally enabled but operations are silently discarded.
     *
     * @return a new runtime instance; call {@link #shutdown()} when done
     */
    public static CodexRuntime inMemory() {
        return inMemory(new NoOpIndexWriter());
    }

    /**
     * Creates a fully wired in-memory runtime with the given {@link IndexWriter}.
     * <p>
     * Pass a {@link codex.codex.internal.index.RecordingIndexWriter} in tests to assert
     * on index upserts. Pass a production writer (OpenSearch, myIR, Lucene) when ready.
     *
     * @param indexWriter the index writer to use; must not be null
     * @return a new runtime instance; call {@link #shutdown()} when done
     */
    public static CodexRuntime inMemory(final IndexWriter indexWriter) {
        Objects.requireNonNull(indexWriter, "indexWriter must not be null");
        LOGGER.info("Starting CodexRuntime (in-memory)");

        final Clock clock = Clock.systemUTC();

        // --- repositories (shared between services and projection subscribers) ---
        final MemorySiteRepository siteRepository = new MemorySiteRepository();
        final MemoryContentTypeRepository contentTypeRepository = new MemoryContentTypeRepository();
        final MemoryContentTypeVersionRepository contentTypeVersionRepository = new MemoryContentTypeVersionRepository();
        final MemoryContentItemRepository contentItemRepository = new MemoryContentItemRepository();
        final MemoryContentRevisionRepository revisionRepository = new MemoryContentRevisionRepository();

        // --- async executor ---
        final CodexExecutor asyncExecutor = CodexExecutor.of(CodexExecutorConfig.of(50));

        // --- indexing projection subscriber ---
        final ContentItemIndexDocumentMapper mapper = new ContentItemIndexDocumentMapper();
        final ContentItemPublishedIndexingSubscriber indexingSubscriber =
                new ContentItemPublishedIndexingSubscriber(
                        contentItemRepository, revisionRepository, indexWriter, mapper);

        // --- event pipeline assembly ---
        // Recording first: events are captured even if a subscriber fails.
        final EventRecorder recorder = new EventRecorder();
        final LocalCodexEventDispatcher localDispatcher =
                new LocalCodexEventDispatcher(List.of(indexingSubscriber));
        final CompositeCodexEventDispatcher composite =
                new CompositeCodexEventDispatcher(List.of(recorder, localDispatcher));
        final DeferredEventDispatcher deferredDispatcher =
                new DeferredEventDispatcher(composite, asyncExecutor);

        // --- services ---
        final SiteIdentityGenerator siteIdentityGenerator = new SiteIdentityGenerator();
        final SiteService siteService = new EventPublishingSiteService(
                new CodexSiteService(siteRepository, clock, siteIdentityGenerator),
                deferredDispatcher, clock);

        final ContentTypeService contentTypeService = new EventPublishingContentTypeService(
                new CodexContentTypeService(contentTypeRepository, contentTypeVersionRepository, clock),
                deferredDispatcher, clock);

        final ContentItemService contentItemService = new EventPublishingContentItemService(
                new CodexContentItemService(contentItemRepository, revisionRepository,
                        contentTypeRepository, contentTypeVersionRepository, clock),
                deferredDispatcher, clock);

        return new CodexRuntime(siteService, contentTypeService, contentItemService,
                deferredDispatcher, recorder, asyncExecutor, clock);
    }

    /**
     * Returns the outermost {@link SiteService} decorator — the primary entry point for
     * site operations.
     *
     * @return the site service; never null
     */
    public SiteService siteService() {
        return siteService;
    }

    /**
     * Returns the {@link ContentTypeService} — the entry point for content type operations.
     *
     * @return the content type service; never null
     */
    public ContentTypeService contentTypeService() {
        return contentTypeService;
    }

    /**
     * Returns the {@link ContentItemService} — the entry point for content item operations.
     *
     * @return the content item service; never null
     */
    public ContentItemService contentItemService() {
        return contentItemService;
    }

    /**
     * Returns the {@link CodexEventDispatcher} used by this runtime.
     * In the in-memory configuration this is a {@link DeferredEventDispatcher}.
     *
     * @return the event dispatcher; never null
     */
    public CodexEventDispatcher eventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Returns the clock used by this runtime.
     *
     * @return the clock; never null
     */
    public Clock clock() {
        return clock;
    }

    /**
     * Returns an immutable snapshot of all events dispatched so far.
     * Primarily useful for observation and testing.
     *
     * @return dispatched events; never null
     */
    public List<CodexEvent> recordedEvents() {
        return eventRecorder.events();
    }

    /**
     * Clears all recorded events.
     * Primarily useful between test steps.
     */
    public void clearRecordedEvents() {
        eventRecorder.clear();
    }

    /**
     * Shuts down the async executor. Idempotent — safe to call multiple times.
     */
    public void shutdown() {
        if (closed.compareAndSet(false, true)) {
            LOGGER.info("Shutting down CodexRuntime");
            asyncExecutor.shutdown();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    // --- inner types ---

    private static final class EventRecorder implements CodexEventDispatcher {

        private final List<CodexEvent> recorded = new ArrayList<>();
        private final StampedLock lock = new StampedLock();

        @Override
        public void dispatch(final CodexEvent event) {
            Objects.requireNonNull(event, "event must not be null");

            final long stamp = lock.writeLock();
            try {
                recorded.add(event);
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        List<CodexEvent> events() {
            final long stamp = lock.readLock();
            try {
                return List.copyOf(recorded);
            } finally {
                lock.unlockRead(stamp);
            }
        }

        void clear() {
            final long stamp = lock.writeLock();
            try {
                recorded.clear();
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }
}
