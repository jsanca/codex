package codex.custos.internal.service;

import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.ContentItemPermissionsService;
import codex.fundamentum.api.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Authorization-enforcing decorator over {@link ContentItemService}.
 * <p>
 * Before delegating each mutating or read operation, this class consults
 * {@link ContentItemPermissionsService} and calls {@link codex.custos.api.model.AccessDecision#requireGranted()}
 * on the result. If the decision is denied, {@link codex.custos.api.exception.AccessDeniedException}
 * is thrown and the delegate is never reached.
 *
 * <p>Destructive operations ({@code delete}, {@code restore}) are <strong>not</strong> delegated.
 * They throw {@link UnsupportedOperationException} until the corresponding permission keys and
 * authorization semantics are defined in the Permissions catalog. This is a fail-closed posture:
 * no unauthenticated path exists through the secured decorator for these operations.
 *
 * <p>List operations ({@code findByContentType}, {@code findAll}) are currently delegated
 * without per-item authorization. A future filtering strategy (post-retrieval or query-level)
 * is needed before these can be considered fully secured.
 *
 * <p>The {@code snapshotProvider} is called once per operation, allowing the caller to
 * supply a fresh {@link PermissionResolutionSnapshot} on each request.
 */
public final class SecuredContentItemService implements ContentItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecuredContentItemService.class);

    private final ContentItemService delegate;
    private final ContentItemPermissionsService permissionsService;
    private final Supplier<PermissionResolutionSnapshot> snapshotProvider;

    /**
     * @param delegate         the service to delegate authorized operations to; must not be null
     * @param permissionsService the permission service used for all authorization checks; must not be null
     * @param snapshotProvider   provides the current permission resolution snapshot; must not be null
     */
    public SecuredContentItemService(final ContentItemService delegate,
                                     final ContentItemPermissionsService permissionsService,
                                     final Supplier<PermissionResolutionSnapshot> snapshotProvider) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.permissionsService = Objects.requireNonNull(permissionsService, "permissionsService must not be null");
        this.snapshotProvider = Objects.requireNonNull(snapshotProvider, "snapshotProvider must not be null");
    }

    @Override
    public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("create: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), command.siteKey().value(), command.contentTypeKey().value());
        permissionsService
                .canCreateContentItem(actor, command.siteKey(), command.contentTypeKey(), snapshotProvider.get())
                .requireGranted();
        return delegate.create(command, actor);
    }

    @Override
    public Optional<ContentItem> findByKey(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                           final ContentItemKey key, final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("findByKey: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value(), key.value());
        permissionsService
                .canReadContentItem(actor, siteKey, contentTypeKey, key, snapshotProvider.get())
                .requireGranted();
        return delegate.findByKey(siteKey, contentTypeKey, key, actor);
    }

    @Override
    public List<ContentItem> findByContentType(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                               final Actor actor) {
        // Future: apply per-item read filtering after retrieval or restrict at query level.
        return delegate.findByContentType(siteKey, contentTypeKey, actor);
    }

    @Override
    public List<ContentItem> findAll(final Actor actor) {
        // Future: apply per-item read filtering after retrieval or restrict at query level.
        return delegate.findAll(actor);
    }

    @Override
    public ContentItem update(final UpdateContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("update: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), command.siteKey().value(), command.contentTypeKey().value(), command.key().value());
        permissionsService
                .canUpdateContentItem(actor, command.siteKey(), command.contentTypeKey(), command.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.update(command, actor);
    }

    @Override
    public ContentItem archive(final ArchiveContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("archive: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), command.siteKey().value(), command.contentTypeKey().value(), command.key().value());
        permissionsService
                .canArchiveContentItem(actor, command.siteKey(), command.contentTypeKey(), command.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.archive(command, actor);
    }

    @Override
    public ContentItem unpublish(final UnpublishContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("unpublish: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), command.siteKey().value(), command.contentTypeKey().value(), command.key().value());
        permissionsService
                .canUnpublishContentItem(actor, command.siteKey(), command.contentTypeKey(), command.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.unpublish(command, actor);
    }

    @Override
    public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        LOGGER.debug("publish: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), command.siteKey().value(), command.contentTypeKey().value(), command.key().value());
        permissionsService
                .canPublishContentItem(actor, command.siteKey(), command.contentTypeKey(), command.key(), snapshotProvider.get())
                .requireGranted();
        return delegate.publish(command, actor);
    }

    @Override
    public void delete(final DeleteContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        throw new UnsupportedOperationException(
                "Secured delete is not supported until content item delete permission semantics are defined");
    }

    @Override
    public ContentItem restore(final RestoreContentItemCommand command, final Actor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        throw new UnsupportedOperationException(
                "Secured restore is not supported until content item restore permission semantics are defined");
    }
}
