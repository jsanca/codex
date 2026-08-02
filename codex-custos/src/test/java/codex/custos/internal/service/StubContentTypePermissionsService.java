package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.ContentTypePermissionsService;
import codex.fundamentum.api.model.Actor;

/**
 * Test double for {@link ContentTypePermissionsService}.
 * <p>
 * Returns a pre-configured {@link AccessDecision} for every {@code can*} method and records
 * the name of the last operation checked. Can be configured to throw a {@link RuntimeException}
 * instead of returning a decision (for invariant propagation tests).
 */
final class StubContentTypePermissionsService implements ContentTypePermissionsService {

    private final AccessDecision stubbedDecision;
    private RuntimeException exceptionToThrow;
    private String lastOperationChecked;

    StubContentTypePermissionsService(final AccessDecision stubbedDecision) {
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
    public AccessDecision canCreateContentType(final Actor actor, final SiteKey siteKey,
                                               final PermissionResolutionSnapshot snapshot) {
        return respond("canCreateContentType");
    }

    @Override
    public AccessDecision canReadContentType(final Actor actor, final SiteKey siteKey,
                                             final ContentTypeKey contentTypeKey,
                                             final PermissionResolutionSnapshot snapshot) {
        return respond("canReadContentType");
    }

    @Override
    public AccessDecision canUpdateContentType(final Actor actor, final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final PermissionResolutionSnapshot snapshot) {
        return respond("canUpdateContentType");
    }

    @Override
    public AccessDecision canArchiveContentType(final Actor actor, final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final PermissionResolutionSnapshot snapshot) {
        return respond("canArchiveContentType");
    }
}
