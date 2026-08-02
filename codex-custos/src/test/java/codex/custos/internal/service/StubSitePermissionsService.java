package codex.custos.internal.service;

import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.SitePermissionsService;
import codex.fundamentum.api.model.Actor;

/**
 * Test double for {@link SitePermissionsService}.
 * <p>
 * Returns a pre-configured {@link AccessDecision} for every {@code can*} method and records
 * the name of the last operation checked. Can be configured to throw a {@link RuntimeException}
 * instead of returning a decision (for invariant propagation tests).
 */
final class StubSitePermissionsService implements SitePermissionsService {

    private final AccessDecision stubbedDecision;
    private RuntimeException exceptionToThrow;
    private String lastOperationChecked;

    StubSitePermissionsService(final AccessDecision stubbedDecision) {
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
    public AccessDecision canCreateSite(final Actor actor, final PermissionResolutionSnapshot snapshot) {
        return respond("canCreateSite");
    }

    @Override
    public AccessDecision canReadSite(final Actor actor, final SiteKey siteKey,
                                      final PermissionResolutionSnapshot snapshot) {
        return respond("canReadSite");
    }

    @Override
    public AccessDecision canStartSite(final Actor actor, final SiteKey siteKey,
                                       final PermissionResolutionSnapshot snapshot) {
        return respond("canStartSite");
    }

    @Override
    public AccessDecision canSuspendSite(final Actor actor, final SiteKey siteKey,
                                         final PermissionResolutionSnapshot snapshot) {
        return respond("canSuspendSite");
    }

    @Override
    public AccessDecision canArchiveSite(final Actor actor, final SiteKey siteKey,
                                         final PermissionResolutionSnapshot snapshot) {
        return respond("canArchiveSite");
    }
}
