package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.ContentItemPermissionsService;
import codex.fundamentum.api.model.Actor;

/**
 * Test double for {@link ContentItemPermissionsService}.
 * <p>
 * Returns a pre-configured {@link AccessDecision} for every {@code can*} method and records
 * the name of the last operation checked. Can be configured to throw a {@link RuntimeException}
 * instead of returning a decision (for invariant propagation tests).
 */
final class StubContentItemPermissionsService implements ContentItemPermissionsService {

    private final AccessDecision stubbedDecision;
    private RuntimeException exceptionToThrow;
    private String lastOperationChecked;

    StubContentItemPermissionsService(final AccessDecision stubbedDecision) {
        this.stubbedDecision = stubbedDecision;
    }

    void throwInstead(final RuntimeException exception) {
        this.exceptionToThrow = exception;
    }

    String lastOperationChecked() {
        return lastOperationChecked;
    }

    private AccessDecision respond(final String operation) {
        this.lastOperationChecked = operation;
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
        return stubbedDecision;
    }

    @Override
    public AccessDecision canCreateContentItem(final Actor actor, final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final PermissionResolutionSnapshot snapshot) {
        return respond("canCreateContentItem");
    }

    @Override
    public AccessDecision canReadContentItem(final Actor actor, final SiteKey siteKey,
                                             final ContentTypeKey contentTypeKey,
                                             final ContentItemKey contentItemKey,
                                             final PermissionResolutionSnapshot snapshot) {
        return respond("canReadContentItem");
    }

    @Override
    public AccessDecision canUpdateContentItem(final Actor actor, final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final ContentItemKey contentItemKey,
                                               final PermissionResolutionSnapshot snapshot) {
        return respond("canUpdateContentItem");
    }

    @Override
    public AccessDecision canPublishContentItem(final Actor actor, final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final ContentItemKey contentItemKey,
                                                final PermissionResolutionSnapshot snapshot) {
        return respond("canPublishContentItem");
    }

    @Override
    public AccessDecision canUnpublishContentItem(final Actor actor, final SiteKey siteKey,
                                                  final ContentTypeKey contentTypeKey,
                                                  final ContentItemKey contentItemKey,
                                                  final PermissionResolutionSnapshot snapshot) {
        return respond("canUnpublishContentItem");
    }

    @Override
    public AccessDecision canArchiveContentItem(final Actor actor, final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final ContentItemKey contentItemKey,
                                                final PermissionResolutionSnapshot snapshot) {
        return respond("canArchiveContentItem");
    }
}
