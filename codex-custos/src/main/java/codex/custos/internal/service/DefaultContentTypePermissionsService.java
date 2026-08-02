package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.AccessDecisionRequest;
import codex.custos.api.model.ContentTypeResourceRef;
import codex.custos.api.model.ContentTypeScope;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Permissions;
import codex.custos.api.model.SiteResourceRef;
import codex.custos.api.model.SiteScope;
import codex.custos.api.service.AccessDecisionService;
import codex.custos.api.service.ContentTypePermissionsService;
import codex.fundamentum.api.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default implementation of {@link ContentTypePermissionsService}.
 * <p>
 * Each method builds the correct {@link codex.custos.api.model.ResourceRef} and
 * {@link codex.custos.api.model.ResourceScope} for the operation, then delegates to the
 * injected {@link AccessDecisionService}. No permission logic lives here.
 */
public final class DefaultContentTypePermissionsService implements ContentTypePermissionsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContentTypePermissionsService.class);

    private final AccessDecisionService decisionService;

    /**
     * @param decisionService the authorization service to delegate to; must not be null
     */
    public DefaultContentTypePermissionsService(final AccessDecisionService decisionService) {
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService must not be null");
    }

    @Override
    public AccessDecision canCreateContentType(final Actor actor, final SiteKey siteKey,
                                               final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canCreateContentType: actor=[{}] site=[{}]", actor.id().value(), siteKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_TYPE_CREATE,
                        new SiteResourceRef(siteKey), new SiteScope(siteKey)),
                snapshot);
    }

    @Override
    public AccessDecision canReadContentType(final Actor actor, final SiteKey siteKey,
                                             final ContentTypeKey contentTypeKey,
                                             final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canReadContentType: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_TYPE_READ,
                        new ContentTypeResourceRef(siteKey, contentTypeKey),
                        new ContentTypeScope(siteKey, contentTypeKey)),
                snapshot);
    }

    @Override
    public AccessDecision canUpdateContentType(final Actor actor, final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canUpdateContentType: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_TYPE_UPDATE,
                        new ContentTypeResourceRef(siteKey, contentTypeKey),
                        new ContentTypeScope(siteKey, contentTypeKey)),
                snapshot);
    }

    @Override
    public AccessDecision canArchiveContentType(final Actor actor, final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canArchiveContentType: actor=[{}] site=[{}] type=[{}]",
                actor.id().value(), siteKey.value(), contentTypeKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.CONTENT_TYPE_ARCHIVE,
                        new ContentTypeResourceRef(siteKey, contentTypeKey),
                        new ContentTypeScope(siteKey, contentTypeKey)),
                snapshot);
    }
}
