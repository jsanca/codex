package codex.concilium.api.runtime;

import codex.chronicon.api.runtime.ChroniconRuntime;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.service.SiteService;
import codex.codex.api.runtime.CodexRuntime;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.AccessDecisionService;
import codex.custos.api.service.ContentItemPermissionsService;
import codex.custos.api.service.ContentTypePermissionsService;
import codex.custos.api.service.SecuredServiceComposer;
import codex.custos.api.service.SitePermissionsService;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.observance.Observance;
import codex.fundamentum.api.runtime.CodexModuleRuntime;
import codex.index.api.runtime.IndexRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Local runtime council for Codex.
 *
 * <p>Composes the canonical core runtime, the indexing projection runtime, and the
 * audit/history projection runtime into a single coherent application runtime. All
 * module subscribers share a unified event dispatcher so that domain events emitted by
 * core services are automatically delivered to Index and Chronicon.</p>
 *
 * <p>Usage (unsecured in-memory, suitable for tests and early development):</p>
 * <pre>{@code
 * ConciliumRuntime runtime = ConciliumRuntime.inMemory();
 * runtime.siteService().create(..., actor);
 * // SiteCreatedEvent reaches ChroniconRuntime subscribers automatically
 * }</pre>
 *
 * <p>Usage (secured with Custos authorization):</p>
 * <pre>{@code
 * Supplier<PermissionResolutionSnapshot> snapshotProvider = () -> currentSnapshot();
 * ConciliumRuntime runtime = ConciliumRuntime.secured(snapshotProvider);
 * runtime.siteService().create(..., actor); // enforces domain authorization
 * }</pre>
 *
 * <p>Usage with custom child runtimes (e.g. recording writers/repositories in tests):</p>
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
    private final SiteService siteService;
    private final ContentTypeService contentTypeService;
    private final ContentItemService contentItemService;
    private final IndexRuntime indexRuntime;
    private final ChroniconRuntime chroniconRuntime;
    private final List<CodexEventSubscriber<? extends CodexEvent>> subscribers;
    private final CodexEventDispatcher eventDispatcher;
    private final RuntimeSecurityMode securityMode;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private ConciliumRuntime(
            final CodexRuntime coreRuntime,
            final SiteService siteService,
            final ContentTypeService contentTypeService,
            final ContentItemService contentItemService,
            final IndexRuntime indexRuntime,
            final ChroniconRuntime chroniconRuntime,
            final List<CodexEventSubscriber<? extends CodexEvent>> subscribers,
            final CodexEventDispatcher eventDispatcher,
            final RuntimeSecurityMode securityMode) {
        this.coreRuntime = coreRuntime;
        this.siteService = siteService;
        this.contentTypeService = contentTypeService;
        this.contentItemService = contentItemService;
        this.indexRuntime = indexRuntime;
        this.chroniconRuntime = chroniconRuntime;
        this.subscribers = subscribers;
        this.eventDispatcher = eventDispatcher;
        this.securityMode = securityMode;
    }

    // --- factories ---

    /**
     * Creates a fully wired in-memory runtime composing all three module runtimes with
     * no-op observance. Services are <strong>unsecured</strong>; no authorization checks
     * are applied. Use {@link #secured(Supplier)} for production or authorization-sensitive paths.
     *
     * <p>Uses a forwarding lambda to break the circular dependency between
     * {@code CodexRuntime} creation (which needs the module dispatcher) and
     * {@code IndexRuntime} creation (which needs the projection reader from
     * {@code CodexRuntime}). The forwarding reference is set once before any
     * service call can occur and is effectively fixed for the lifetime of the runtime.</p>
     *
     * @return a new, fully assembled {@code ConciliumRuntime}
     */
    public static ConciliumRuntime inMemory() {
        return inMemory(Observance.noop());
    }

    /**
     * Creates a fully wired in-memory runtime composing all three module runtimes,
     * wiring the same {@link Observance} instance through the full event stack.
     * Services are <strong>unsecured</strong>; no authorization checks are applied.
     *
     * <p>The provided {@code Observance} is passed to:</p>
     * <ul>
     *   <li>{@code DeferredEventDispatcher} — for deferred-event buffering and commit metrics</li>
     *   <li>{@code LocalCodexEventDispatcher} (module dispatcher) — for dispatch and subscriber metrics</li>
     *   <li>{@code IndexRuntime} / {@code ObservingIndexWriter} — for index upsert/delete metrics</li>
     * </ul>
     *
     * <p>Uses a forwarding lambda to break the circular dependency between
     * {@code CodexRuntime} creation (which needs the module dispatcher) and
     * {@code IndexRuntime} creation (which needs the projection reader from
     * {@code CodexRuntime}). The forwarding reference is set once before any
     * service call can occur and is effectively fixed for the lifetime of the runtime.</p>
     *
     * @param observance the observance instance to wire through all layers; must not be null
     * @return a new, fully assembled {@code ConciliumRuntime}
     */
    public static ConciliumRuntime inMemory(final Observance observance) {
        Objects.requireNonNull(observance, "observance must not be null");
        LOGGER.info("Assembling ConciliumRuntime (in-memory)");

        final AtomicReference<CodexEventDispatcher> placeholder =
                new AtomicReference<>(event -> {});
        final CodexRuntime core = CodexRuntime.inMemory(
                event -> placeholder.get().dispatch(event), observance);

        final IndexRuntime index = IndexRuntime.inMemory(
                core.contentItemProjectionReader(), observance);
        final ChroniconRuntime chronicon = ChroniconRuntime.inMemory();

        final List<CodexEventSubscriber<? extends CodexEvent>> allSubscribers =
                buildSubscriberList(index, chronicon);
        final LocalCodexEventDispatcher moduleDispatcher =
                new LocalCodexEventDispatcher(allSubscribers, observance);
        placeholder.set(moduleDispatcher);

        LOGGER.info("ConciliumRuntime ready [{}]: {} subscribers wired",
                RuntimeSecurityMode.UNSECURED, allSubscribers.size());
        return new ConciliumRuntime(core,
                core.siteService(), core.contentTypeService(), core.contentItemService(),
                index, chronicon, allSubscribers, moduleDispatcher, RuntimeSecurityMode.UNSECURED);
    }

    /**
     * Creates a fully wired in-memory runtime with Custos authorization enforced on all
     * service operations. The {@code snapshotProvider} is invoked once per secured operation
     * to supply the current {@link PermissionResolutionSnapshot}; it is never called eagerly
     * at runtime creation time.
     *
     * <p>The authorization stack uses the default Custos resolver and decision service.
     * The event pipeline (Index + Chronicon subscribers) is identical to {@link #inMemory()} —
     * authorized operations that mutate state still emit domain events.</p>
     *
     * <p>Use {@link #inMemory()} for unsecured paths (core unit tests, back-compat).</p>
     *
     * @param snapshotProvider supplier that returns the active permission snapshot for each
     *                         operation; must not be null; called per secured service call
     * @return a new, fully assembled secured {@code ConciliumRuntime}
     */
    public static ConciliumRuntime secured(final Supplier<PermissionResolutionSnapshot> snapshotProvider) {
        Objects.requireNonNull(snapshotProvider, "snapshotProvider must not be null");
        LOGGER.info("Assembling ConciliumRuntime (secured)");

        final AtomicReference<CodexEventDispatcher> placeholder =
                new AtomicReference<>(event -> {});
        final CodexRuntime core = CodexRuntime.inMemory(
                event -> placeholder.get().dispatch(event), Observance.noop());

        final IndexRuntime index = IndexRuntime.inMemory(
                core.contentItemProjectionReader(), Observance.noop());
        final ChroniconRuntime chronicon = ChroniconRuntime.inMemory();

        final List<CodexEventSubscriber<? extends CodexEvent>> allSubscribers =
                buildSubscriberList(index, chronicon);
        final LocalCodexEventDispatcher moduleDispatcher =
                new LocalCodexEventDispatcher(allSubscribers, Observance.noop());
        placeholder.set(moduleDispatcher);

        final AccessDecisionService decisionService = SecuredServiceComposer.defaultAccessDecisionService();
        final SitePermissionsService sitePerms =
                SecuredServiceComposer.defaultSitePermissionsService(decisionService);
        final ContentTypePermissionsService ctypePerms =
                SecuredServiceComposer.defaultContentTypePermissionsService(decisionService);
        final ContentItemPermissionsService citemPerms =
                SecuredServiceComposer.defaultContentItemPermissionsService(decisionService);

        final SiteService securedSite =
                SecuredServiceComposer.wrapSiteService(core.siteService(), sitePerms, snapshotProvider);
        final ContentTypeService securedCtype =
                SecuredServiceComposer.wrapContentTypeService(core.contentTypeService(), ctypePerms, snapshotProvider);
        final ContentItemService securedCitem =
                SecuredServiceComposer.wrapContentItemService(core.contentItemService(), citemPerms, snapshotProvider);

        LOGGER.info("ConciliumRuntime ready [{}]: {} subscribers wired",
                RuntimeSecurityMode.SECURED, allSubscribers.size());
        return new ConciliumRuntime(core, securedSite, securedCtype, securedCitem,
                index, chronicon, allSubscribers, moduleDispatcher, RuntimeSecurityMode.SECURED);
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

        return new ConciliumRuntime(coreRuntime,
                coreRuntime.siteService(), coreRuntime.contentTypeService(), coreRuntime.contentItemService(),
                indexRuntime, chroniconRuntime, allSubscribers, moduleDispatcher, RuntimeSecurityMode.UNSECURED);
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

    // --- service accessors ---

    /**
     * Returns the active {@link SiteService} for this runtime.
     *
     * <p>For runtimes created with {@link #secured(Supplier)}, this is the
     * authorization-enforcing decorator. For runtimes created with {@link #inMemory()} or
     * {@link #compose(CodexRuntime, IndexRuntime, ChroniconRuntime)}, this is the raw
     * service from the core runtime.</p>
     *
     * @return the site service; never null
     */
    public SiteService siteService() {
        return siteService;
    }

    /**
     * Returns the active {@link ContentTypeService} for this runtime.
     *
     * <p>For runtimes created with {@link #secured(Supplier)}, this is the
     * authorization-enforcing decorator. For unsecured runtimes, this is the raw service.</p>
     *
     * @return the content-type service; never null
     */
    public ContentTypeService contentTypeService() {
        return contentTypeService;
    }

    /**
     * Returns the active {@link ContentItemService} for this runtime.
     *
     * <p>For runtimes created with {@link #secured(Supplier)}, this is the
     * authorization-enforcing decorator. For unsecured runtimes, this is the raw service.</p>
     *
     * @return the content-item service; never null
     */
    public ContentItemService contentItemService() {
        return contentItemService;
    }

    // --- module runtime accessors ---

    /**
     * Returns the underlying raw Codex runtime.
     *
     * <p><strong>Security note:</strong> On secured Concilium runtimes, services obtained
     * through this raw runtime bypass Custos authorization. Domain callers, adapters,
     * and external entry points should use {@link #siteService()},
     * {@link #contentTypeService()}, and {@link #contentItemService()} instead.</p>
     *
     * <p>This accessor exists for lower-level runtime concerns such as projection
     * readers, recorded events, lifecycle coordination, and internal composition.</p>
     *
     * @return the underlying raw Codex runtime; never null
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

    /**
     * Returns the security mode of this runtime.
     *
     * <p>This is diagnostic metadata only — security is enforced by the service graph
     * (secured vs. unsecured decorators), not by this value alone.</p>
     *
     * @return {@link RuntimeSecurityMode#SECURED} if Custos authorization is active;
     *         {@link RuntimeSecurityMode#UNSECURED} otherwise
     */
    public RuntimeSecurityMode securityMode() {
        return securityMode;
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
