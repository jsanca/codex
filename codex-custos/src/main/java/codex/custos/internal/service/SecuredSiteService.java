package codex.custos.internal.service;

import codex.codex.api.model.command.ArchiveSiteCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.StartSiteCommand;
import codex.codex.api.model.command.SuspendSiteCommand;
import codex.codex.api.model.command.UnarchiveSiteCommand;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.SitePermissionsService;
import codex.fundamentum.api.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Authorization-enforcing decorator over {@link SiteService}.
 * <p>
 * Before delegating each mutating or keyed-read operation, this class consults
 * {@link SitePermissionsService} and calls {@link codex.custos.api.model.AccessDecision#requireGranted()}
 * on the result. If the decision is denied, {@link codex.custos.api.exception.AccessDeniedException}
 * is thrown and the delegate is never reached.
 *
 * <p>{@code unarchive} is <strong>not</strong> delegated. It throws
 * {@link UnsupportedOperationException} until {@code canUnarchiveSite} and its corresponding
 * permission key are defined in the Permissions catalog. This is a fail-closed posture.
 *
 * <p>{@code findByAlias} and {@code findAll} are currently delegated without authorization.
 * {@code findByAlias} cannot be checked against {@code canReadSite} without first resolving the
 * alias to a {@link SiteKey} — a future two-phase approach is needed. {@code findAll} requires a
 * post-retrieval or query-level filtering strategy.
 *
 * <p>The {@code snapshotProvider} is called once per operation, allowing the caller to
 * supply a fresh {@link PermissionResolutionSnapshot} on each request.
 */
public final class SecuredSiteService implements SiteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecuredSiteService.class);

    private final SiteService delegate;
    private final SitePermissionsService permissionsService;
    private final Supplier<PermissionResolutionSnapshot> snapshotProvider;

    /**
     * @param delegate           the service to delegate authorized operations to; must not be null
     * @param permissionsService the permission service used for all authorization checks; must not be null
     * @param snapshotProvider   provides the current permission resolution snapshot; must not be null
     */
    public SecuredSiteService(final SiteService delegate,
                              final SitePermissionsService permissionsService,
                              final Supplier<PermissionResolutionSnapshot> snapshotProvider) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.permissionsService = Objects.requireNonNull(permissionsService, "permissionsService must not be null");
        this.snapshotProvider = Objects.requireNonNull(snapshotProvider, "snapshotProvider must not be null");
    }

    @Override
    public Site create(final CreateSiteCommand createSiteCommand, final Actor actor) {
        Objects.requireNonNull(createSiteCommand, "createSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("create: actor=[{}] site=[{}]", actor.id().value(), createSiteCommand.key().value());
        permissionsService
                .canCreateSite(actor, snapshotProvider.get())
                .requireGranted();
        return delegate.create(createSiteCommand, actor);
    }

    @Override
    public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("findByKey: actor=[{}] site=[{}]", actor.id().value(), siteKey.value());
        permissionsService
                .canReadSite(actor, siteKey, snapshotProvider.get())
                .requireGranted();
        return delegate.findByKey(siteKey, actor);
    }

    @Override
    public Site start(final StartSiteCommand startSiteCommand, final Actor actor) {
        Objects.requireNonNull(startSiteCommand, "startSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("start: actor=[{}] site=[{}]", actor.id().value(), startSiteCommand.key().value());
        permissionsService
                .canStartSite(actor, startSiteCommand.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.start(startSiteCommand, actor);
    }

    @Override
    public Site suspend(final SuspendSiteCommand suspendSiteCommand, final Actor actor) {
        Objects.requireNonNull(suspendSiteCommand, "suspendSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("suspend: actor=[{}] site=[{}]", actor.id().value(), suspendSiteCommand.key().value());
        permissionsService
                .canSuspendSite(actor, suspendSiteCommand.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.suspend(suspendSiteCommand, actor);
    }

    @Override
    public Site archive(final ArchiveSiteCommand archiveSiteCommand, final Actor actor) {
        Objects.requireNonNull(archiveSiteCommand, "archiveSiteCommand must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("archive: actor=[{}] site=[{}]", actor.id().value(), archiveSiteCommand.key().value());
        permissionsService
                .canArchiveSite(actor, archiveSiteCommand.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.archive(archiveSiteCommand, actor);
    }

    @Override
    public Site unarchive(final UnarchiveSiteCommand unarchiveSiteCommand, final Actor actor) {
        throw new UnsupportedOperationException(
                "Secured unarchive is not supported until site unarchive permission semantics are defined");
    }

    @Override
    public Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
        // Future: resolve alias to SiteKey first, then check canReadSite before delegating.
        return delegate.findByAlias(alias, actor);
    }

    @Override
    public List<Site> findAll(final Actor actor) {
        // Future: apply per-item read filtering after retrieval or restrict at query level.
        return delegate.findAll(actor);
    }
}
