package codex.codex.internal.runtime;

import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.service.SiteService;
import codex.codex.internal.repository.MemoryContentItemRepository;
import codex.codex.internal.repository.MemoryContentTypeRepository;
import codex.codex.internal.repository.MemoryContentTypeVersionRepository;
import codex.codex.internal.repository.MemorySiteRepository;
import codex.codex.internal.service.CodexContentItemService;
import codex.codex.internal.service.EventPublishingContentItemService;
import codex.codex.internal.service.CodexContentTypeService;
import codex.codex.internal.service.CodexSiteService;
import codex.codex.internal.service.DeferredEventDispatcher;
import codex.codex.internal.service.EventPublishingContentTypeService;
import codex.codex.internal.service.EventPublishingSiteService;
import codex.codex.internal.service.SiteIdentityGenerator;
import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.concurrent.CodexExecutorConfig;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
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
     * Creates a fully wired in-memory runtime suitable for local development and testing.
     *
     * <p>Pipeline assembled:</p>
     * <pre>
     * MemorySiteRepository
     *   ← CodexSiteService
     *     ← EventPublishingSiteService
     *         ← DeferredEventDispatcher
     *             ← EventRecorder (inner recording delegate)
     *
     * MemoryContentTypeRepository + MemoryContentTypeVersionRepository
     *   ← CodexContentTypeService
     *     ← EventPublishingContentTypeService
     *
     * MemoryContentItemRepository
     *   ← CodexContentItemService
     *     ← EventPublishingContentItemService
     * </pre>
     *
     * @return a new runtime instance; call {@link #shutdown()} when done
     */
    public static CodexRuntime inMemory() {
        LOGGER.info("Starting CodexRuntime (in-memory)");

        final Clock clock = Clock.systemUTC();

        final MemorySiteRepository siteRepository = new MemorySiteRepository();
        final SiteIdentityGenerator siteIdentityGenerator = new SiteIdentityGenerator();
        final CodexSiteService coreSiteService = new CodexSiteService(siteRepository, clock, siteIdentityGenerator);
        final EventRecorder recorder = new EventRecorder();
        final CodexExecutor asyncExecutor = CodexExecutor.of(CodexExecutorConfig.of(50));
        final DeferredEventDispatcher deferredDispatcher = new DeferredEventDispatcher(recorder, asyncExecutor);
        final SiteService siteService = new EventPublishingSiteService(coreSiteService, deferredDispatcher, clock);

        final MemoryContentTypeRepository contentTypeRepository = new MemoryContentTypeRepository();
        final MemoryContentTypeVersionRepository contentTypeVersionRepository = new MemoryContentTypeVersionRepository();
        final CodexContentTypeService coreContentTypeService = new CodexContentTypeService(
                contentTypeRepository, contentTypeVersionRepository, clock);
        final ContentTypeService contentTypeService = new EventPublishingContentTypeService(coreContentTypeService, deferredDispatcher, clock);

        final MemoryContentItemRepository contentItemRepository = new MemoryContentItemRepository();
        final ContentItemService contentItemService = new EventPublishingContentItemService(
                new CodexContentItemService(contentItemRepository, contentTypeRepository, contentTypeVersionRepository, clock),
                deferredDispatcher,
                clock);

        return new CodexRuntime(siteService, contentTypeService, contentItemService, deferredDispatcher, recorder, asyncExecutor, clock);
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
