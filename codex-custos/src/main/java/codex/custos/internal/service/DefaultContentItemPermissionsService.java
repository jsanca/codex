package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.AccessDecisionRequest;
import codex.custos.api.model.ContentItemResourceRef;
import codex.custos.api.model.ContentItemScope;
import codex.custos.api.model.ContentTypeResourceRef;
import codex.custos.api.model.ContentTypeScope;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Permissions;
import codex.custos.api.service.AccessDecisionService;
import codex.custos.api.service.ContentItemPermissionsService;
import codex.fundamentum.api.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default implementation of {@link ContentItemPermissionsService}.
 * <p>
 * Each method builds the correct {@link codex.custos.api.model.ResourceRef} and
 * {@link codex.custos.api.model.ResourceScope} for the operation, then delegates to the
 * injected {@link AccessDecisionService}. No permission logic lives here.
 */
public final class DefaultContentItemPermissionsService implements ContentItemPermissionsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContentItemPermissionsService.class);

    private final AccessDecisionService decisionService;

    /**
     * @param decisionService the authorization service to delegate to; must not be null
     */
    public DefaultContentItemPermissionsService(final AccessDecisionService decisionService) {
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService must not be null");
    }

    @Override
    public AccessDecision canCreateContentItem(final Actor actor, final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canCreateContentItem: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_ITEM_CREATE,
                        new ContentTypeResourceRef(siteKey, contentTypeKey),
                        new ContentTypeScope(siteKey, contentTypeKey)),
                snapshot);
    }

    @Override
    public AccessDecision canReadContentItem(final Actor actor, final SiteKey siteKey,
                                             final ContentTypeKey contentTypeKey,
                                             final ContentItemKey contentItemKey,
                                             final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canReadContentItem: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value(), contentItemKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_ITEM_READ,
                        new ContentItemResourceRef(siteKey, contentTypeKey, contentItemKey),
                        new ContentItemScope(siteKey, contentTypeKey, contentItemKey)),
                snapshot);
    }

    @Override
    public AccessDecision canUpdateContentItem(final Actor actor, final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final ContentItemKey contentItemKey,
                                               final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canUpdateContentItem: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value(), contentItemKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_ITEM_UPDATE,
                        new ContentItemResourceRef(siteKey, contentTypeKey, contentItemKey),
                        new ContentItemScope(siteKey, contentTypeKey, contentItemKey)),
                snapshot);
    }

    @Override
    public AccessDecision canPublishContentItem(final Actor actor, final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final ContentItemKey contentItemKey,
                                                final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canPublishContentItem: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value(), contentItemKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_ITEM_PUBLISH,
                        new ContentItemResourceRef(siteKey, contentTypeKey, contentItemKey),
                        new ContentItemScope(siteKey, contentTypeKey, contentItemKey)),
                snapshot);
    }

    @Override
    public AccessDecision canUnpublishContentItem(final Actor actor, final SiteKey siteKey,
                                                  final ContentTypeKey contentTypeKey,
                                                  final ContentItemKey contentItemKey,
                                                  final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canUnpublishContentItem: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value(), contentItemKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_ITEM_UNPUBLISH,
                        new ContentItemResourceRef(siteKey, contentTypeKey, contentItemKey),
                        new ContentItemScope(siteKey, contentTypeKey, contentItemKey)),
                snapshot);
    }

    @Override
    public AccessDecision canArchiveContentItem(final Actor actor, final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final ContentItemKey contentItemKey,
                                                final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canArchiveContentItem: actor=[{}] site=[{}] type=[{}] item=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value(), contentItemKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_ITEM_ARCHIVE,
                        new ContentItemResourceRef(siteKey, contentTypeKey, contentItemKey),
                        new ContentItemScope(siteKey, contentTypeKey, contentItemKey)),
                snapshot);
    }
}
