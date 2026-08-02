package codex.custos.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentTypeService;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.ContentTypePermissionsService;
import codex.fundamentum.api.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Authorization-enforcing decorator over {@link ContentTypeService}.
 * <p>
 * Before delegating each mutating or read operation, this class consults
 * {@link ContentTypePermissionsService} and calls {@link codex.custos.api.model.AccessDecision#requireGranted()}
 * on the result. If the decision is denied, {@link codex.custos.api.exception.AccessDeniedException}
 * is thrown and the delegate is never reached.
 *
 * <p>Schema-mutation operations ({@code activate}, {@code addField}, {@code removeField}) are
 * checked against {@code canUpdateContentType} — they modify the content type but are not
 * represented by a separate permission in the current vocabulary.
 *
 * <p>List operations ({@code findBySiteKey}, {@code findAll}) are currently delegated
 * without per-item authorization. A future filtering strategy (post-retrieval or query-level)
 * is needed before these can be considered fully secured.
 *
 * <p>The {@code snapshotProvider} is called once per operation, allowing the caller to
 * supply a fresh {@link PermissionResolutionSnapshot} on each request.
 */
public final class SecuredContentTypeService implements ContentTypeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecuredContentTypeService.class);

    private final ContentTypeService delegate;
    private final ContentTypePermissionsService permissionsService;
    private final Supplier<PermissionResolutionSnapshot> snapshotProvider;

    /**
     * @param delegate           the service to delegate authorized operations to; must not be null
     * @param permissionsService the permission service used for all authorization checks; must not be null
     * @param snapshotProvider   provides the current permission resolution snapshot; must not be null
     */
    public SecuredContentTypeService(final ContentTypeService delegate,
                                     final ContentTypePermissionsService permissionsService,
                                     final Supplier<PermissionResolutionSnapshot> snapshotProvider) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.permissionsService = Objects.requireNonNull(permissionsService, "permissionsService must not be null");
        this.snapshotProvider = Objects.requireNonNull(snapshotProvider, "snapshotProvider must not be null");
    }

    @Override
    public ContentType create(final CreateContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("create: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), command.siteKey().value(), command.key().value());
        permissionsService
                .canCreateContentType(actor, command.siteKey(), snapshotProvider.get())
                .requireGranted();
        return delegate.create(command, actor);
    }

    @Override
    public ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("activate: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), command.siteKey().value(), command.key().value());
        permissionsService
                .canUpdateContentType(actor, command.siteKey(), command.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.activate(command, actor);
    }

    @Override
    public ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("archive: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), command.siteKey().value(), command.key().value());
        permissionsService
                .canArchiveContentType(actor, command.siteKey(), command.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.archive(command, actor);
    }

    @Override
    public Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key, final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("findByKey: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), siteKey.value(), key.value());
        permissionsService
                .canReadContentType(actor, siteKey, key, snapshotProvider.get())
                .requireGranted();
        return delegate.findByKey(siteKey, key, actor);
    }

    @Override
    public List<ContentType> findBySiteKey(final SiteKey siteKey, final Actor actor) {
        // Future: apply per-item read filtering after retrieval or restrict at query level.
        return delegate.findBySiteKey(siteKey, actor);
    }

    @Override
    public List<ContentType> findAll(final Actor actor) {
        // Future: apply per-item read filtering after retrieval or restrict at query level.
        return delegate.findAll(actor);
    }

    @Override
    public ContentType addField(final AddContentTypeFieldCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("addField: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), command.siteKey().value(), command.contentTypeKey().value());
        permissionsService
                .canUpdateContentType(actor, command.siteKey(), command.contentTypeKey(), snapshotProvider.get())
                .requireGranted();
        return delegate.addField(command, actor);
    }

    @Override
    public ContentType removeField(final RemoveContentTypeFieldCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("removeField: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), command.siteKey().value(), command.contentTypeKey().value());
        permissionsService
                .canUpdateContentType(actor, command.siteKey(), command.contentTypeKey(), snapshotProvider.get())
                .requireGranted();
        return delegate.removeField(command, actor);
    }
}
