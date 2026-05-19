package codex.codex.api.runtime;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.service.SiteService;
import codex.codex.api.projection.ContentItemProjectionReader;
import codex.codex.internal.cache.ContentItemArchivedCacheInvalidationSubscriber;
import codex.codex.internal.cache.ContentItemCacheKey;
import codex.codex.internal.cache.ContentItemCreatedCacheInvalidationSubscriber;
import codex.codex.internal.cache.ContentItemDeletedCacheInvalidationSubscriber;
import codex.codex.internal.cache.ContentItemPublishedCacheInvalidationSubscriber;
import codex.codex.internal.cache.ContentItemRestoredCacheInvalidationSubscriber;
import codex.codex.internal.cache.ContentItemUnpublishedCacheInvalidationSubscriber;
import codex.codex.internal.cache.ContentItemUpdatedCacheInvalidationSubscriber;
import codex.codex.internal.projection.RepositoryContentItemProjectionReader;
import codex.codex.internal.repository.MemoryContentItemRepository;
import codex.codex.internal.repository.MemoryContentRevisionRepository;
import codex.codex.internal.repository.MemoryContentTypeRepository;
import codex.codex.internal.repository.MemoryContentTypeVersionRepository;
import codex.codex.internal.repository.MemorySiteRepository;
import codex.codex.internal.service.CachingContentItemService;
import codex.codex.internal.service.CodexContentItemService;
import codex.codex.internal.service.CodexContentTypeService;
import codex.codex.internal.service.CodexSiteService;
import codex.codex.internal.service.DeferredEventDispatcher;
import codex.codex.internal.service.EventPublishingContentItemService;
import codex.codex.internal.service.EventPublishingContentTypeService;
import codex.codex.internal.service.EventPublishingSiteService;
import codex.codex.internal.service.SiteIdentityGenerator;
import codex.codex.internal.service.TimedContentItemService;
import codex.codex.internal.service.TimedContentTypeService;
import codex.codex.internal.service.TimedSiteService;
import codex.fundamentum.api.cache.CacheRegion;
import codex.fundamentum.api.cache.ConcurrentMapCacheRegion;
import codex.fundamentum.api.cache.ObservingCacheRegion;
import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.concurrent.CodexExecutorConfig;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.event.CompositeCodexEventDispatcher;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.observance.Observance;
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
 * <p>Event pipeline (in-memory without external dispatcher):</p>
 * <pre>
 * DeferredEventDispatcher (transaction-aware)
 *   → CompositeCodexEventDispatcher
 *      → EventRecorder              (domain event recording — first so events survive subscriber failures)
 *      → LocalCodexEventDispatcher  (ContentItem cache invalidation subscribers)
 *      → externalDispatcher         (no-op by default; Concilium provides module subscribers here)
 * </pre>
 *
 * @author jsanca &amp; clio
 */
public final class CodexRuntime implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodexRuntime.class);

    private final SiteService siteService;
    private final ContentTypeService contentTypeService;
    private final ContentItemService contentItemService;
    private final ContentItemProjectionReader contentItemProjectionReader;
    private final CodexEventDispatcher eventDispatcher;
    private final EventRecorder eventRecorder;
    private final CodexExecutor asyncExecutor;
    private final Clock clock;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private CodexRuntime(final SiteService siteService,
                         final ContentTypeService contentTypeService,
                         final ContentItemService contentItemService,
                         final ContentItemProjectionReader contentItemProjectionReader,
                         final CodexEventDispatcher eventDispatcher,
                         final EventRecorder eventRecorder,
                         final CodexExecutor asyncExecutor,
                         final Clock clock) {
        this.siteService = Objects.requireNonNull(siteService);
        this.contentTypeService = Objects.requireNonNull(contentTypeService);
        this.contentItemService = Objects.requireNonNull(contentItemService);
        this.contentItemProjectionReader = Objects.requireNonNull(contentItemProjectionReader);
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher);
        this.eventRecorder = Objects.requireNonNull(eventRecorder);
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Creates a fully wired in-memory runtime with no external event subscribers.
     *
     * <p>Module subscribers (indexing, chronicon) are not wired here — they are composed by
     * {@code codex-concilium} using {@link #inMemory(CodexEventDispatcher)}.</p>
     *
     * @return a new runtime instance; call {@link #shutdown()} when done
     */
    public static CodexRuntime inMemory() {
        return inMemory(event -> {});
    }

    /**
     * Creates a fully wired in-memory runtime with service and deferred-event metrics
     * captured via the provided {@link Observance}.
     *
     * <p>Uses a no-op external event dispatcher — module subscribers (indexing, chronicon)
     * are not wired. For full module composition with observance, see
     * {@code ConciliumRuntime.inMemory(Observance)}.</p>
     *
     * @param observance observance for service-level and deferred-event metrics; must not be null
     * @return a new runtime instance; call {@link #shutdown()} when done
     */
    public static CodexRuntime inMemory(final Observance observance) {
        Objects.requireNonNull(observance, "observance must not be null");
        return inMemory(event -> {}, observance);
    }

    /**
     * Creates a fully wired in-memory runtime that routes domain events to the provided
     * external dispatcher after the internal {@code EventRecorder}.
     *
     * <p>Intended for use by {@code codex-concilium} to inject module subscribers
     * (indexing, chronicon) into the core event pipeline without making {@code codex-codex}
     * depend on those modules.</p>
     *
     * @param externalDispatcher the dispatcher to receive all domain events after recording;
     *                           must not be null
     * @return a new runtime instance; call {@link #shutdown()} when done
     */
    public static CodexRuntime inMemory(final CodexEventDispatcher externalDispatcher) {
        return inMemory(externalDispatcher, Observance.noop());
    }

    /**
     * Creates a fully wired in-memory runtime that routes domain events to the provided
     * external dispatcher, and captures deferred-dispatch metrics using the provided
     * {@link Observance}.
     *
     * <p>Intended for use by {@code codex-concilium} when a single {@code Observance}
     * instance should observe both the core deferred-dispatch pipeline and the module
     * subscriber layer.</p>
     *
     * @param externalDispatcher the dispatcher to receive all domain events after recording;
     *                           must not be null
     * @param observance         observance for deferred-event metrics; must not be null
     * @return a new runtime instance; call {@link #shutdown()} when done
     */
    public static CodexRuntime inMemory(
            final CodexEventDispatcher externalDispatcher,
            final Observance observance) {
        Objects.requireNonNull(externalDispatcher, "externalDispatcher must not be null");
        Objects.requireNonNull(observance, "observance must not be null");
        LOGGER.info("Starting CodexRuntime (in-memory)");
        return assemble(externalDispatcher, observance);
    }

    private static CodexRuntime assemble(
            final CodexEventDispatcher externalDispatcher,
            final Observance observance) {
        final Clock clock = Clock.systemUTC();

        // --- repositories ---
        final MemorySiteRepository siteRepository = new MemorySiteRepository();
        final MemoryContentTypeRepository contentTypeRepository = new MemoryContentTypeRepository();
        final MemoryContentTypeVersionRepository contentTypeVersionRepository = new MemoryContentTypeVersionRepository();
        final MemoryContentItemRepository contentItemRepository = new MemoryContentItemRepository();
        final MemoryContentRevisionRepository revisionRepository = new MemoryContentRevisionRepository();

        // --- projection reader ---
        final ContentItemProjectionReader projectionReader =
                new RepositoryContentItemProjectionReader(contentItemRepository, revisionRepository);

        // --- async executor ---
        final CodexExecutor asyncExecutor = CodexExecutor.of(CodexExecutorConfig.of(50));

        // --- content item cache ---
        final CacheRegion<ContentItemCacheKey, ContentItem> contentItemCache =
                new ObservingCacheRegion<>(new ConcurrentMapCacheRegion<>(), "contentItem", observance);

        final LocalCodexEventDispatcher cacheDispatcher = new LocalCodexEventDispatcher(List.of(
                new ContentItemCreatedCacheInvalidationSubscriber(contentItemCache),
                new ContentItemUpdatedCacheInvalidationSubscriber(contentItemCache),
                new ContentItemPublishedCacheInvalidationSubscriber(contentItemCache),
                new ContentItemUnpublishedCacheInvalidationSubscriber(contentItemCache),
                new ContentItemArchivedCacheInvalidationSubscriber(contentItemCache),
                new ContentItemRestoredCacheInvalidationSubscriber(contentItemCache),
                new ContentItemDeletedCacheInvalidationSubscriber(contentItemCache)
        ), observance);

        // --- event pipeline assembly ---
        // Recording first: events are captured even if a subscriber fails.
        // Cache invalidation second: evictions happen before external module subscribers run.
        final EventRecorder recorder = new EventRecorder();
        final CompositeCodexEventDispatcher composite =
                new CompositeCodexEventDispatcher(List.of(recorder, cacheDispatcher, externalDispatcher));
        final DeferredEventDispatcher deferredDispatcher =
                new DeferredEventDispatcher(composite, asyncExecutor, observance);

        // --- services ---
        final SiteIdentityGenerator siteIdentityGenerator = new SiteIdentityGenerator();
        final SiteService siteService = new TimedSiteService(
                new EventPublishingSiteService(
                        new CodexSiteService(siteRepository, clock, siteIdentityGenerator),
                        deferredDispatcher, clock),
                observance);

        final ContentTypeService contentTypeService = new TimedContentTypeService(
                new EventPublishingContentTypeService(
                        new CodexContentTypeService(contentTypeRepository, contentTypeVersionRepository, clock),
                        deferredDispatcher, clock),
                observance);

        final ContentItemService contentItemService = new TimedContentItemService(
                new EventPublishingContentItemService(
                        new CachingContentItemService(
                                new CodexContentItemService(contentItemRepository, revisionRepository,
                                        contentTypeRepository, contentTypeVersionRepository, clock),
                                contentItemCache),
                        deferredDispatcher, clock),
                observance);

        return new CodexRuntime(siteService, contentTypeService, contentItemService,
                projectionReader, deferredDispatcher, recorder, asyncExecutor, clock);
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
     * Returns the {@link ContentItemProjectionReader} — the public projection/read contract
     * for modules that need canonical content state without depending on internal repositories.
     *
     * <p>Intended for use by runtime assembly layers to wire projection modules such as
     * {@code codex-index} or {@code codex-chronicon}.</p>
     *
     * @return the projection reader; never null
     */
    public ContentItemProjectionReader contentItemProjectionReader() {
        return contentItemProjectionReader;
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
