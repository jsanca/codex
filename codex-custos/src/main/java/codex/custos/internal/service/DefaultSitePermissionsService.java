package codex.custos.internal.service;

import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.AccessDecisionRequest;
import codex.custos.api.model.GlobalResourceRef;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Permissions;
import codex.custos.api.model.SiteResourceRef;
import codex.custos.api.model.SiteScope;
import codex.custos.api.service.AccessDecisionService;
import codex.custos.api.service.SitePermissionsService;
import codex.fundamentum.api.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default implementation of {@link SitePermissionsService}.
 * <p>
 * Each method builds the correct {@link codex.custos.api.model.ResourceRef} and
 * {@link codex.custos.api.model.ResourceScope} for the operation, then delegates to the
 * injected {@link AccessDecisionService}. No permission logic lives here.
 */
public final class DefaultSitePermissionsService implements SitePermissionsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSitePermissionsService.class);

    private final AccessDecisionService decisionService;

    /**
     * @param decisionService the authorization service to delegate to; must not be null
     */
    public DefaultSitePermissionsService(final AccessDecisionService decisionService) {
        this.decisionService = Objects.requireNonNull(decisionService, "decisionService must not be null");
    }

    @Override
    public AccessDecision canCreateSite(final Actor actor, final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canCreateSite: actor=[{}]", actor.id().value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.SITE_CREATE,
                        GlobalResourceRef.INSTANCE, GlobalScope.INSTANCE),
                snapshot);
    }

    @Override
    public AccessDecision canReadSite(final Actor actor, final SiteKey siteKey,
                                      final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canReadSite: actor=[{}] site=[{}]", actor.id().value(), siteKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.SITE_READ,
                        new SiteResourceRef(siteKey), new SiteScope(siteKey)),
                snapshot);
    }

    @Override
    public AccessDecision canStartSite(final Actor actor, final SiteKey siteKey,
                                       final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canStartSite: actor=[{}] site=[{}]", actor.id().value(), siteKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.SITE_START,
                        new SiteResourceRef(siteKey), new SiteScope(siteKey)),
                snapshot);
    }

    @Override
    public AccessDecision canSuspendSite(final Actor actor, final SiteKey siteKey,
                                         final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canSuspendSite: actor=[{}] site=[{}]", actor.id().value(), siteKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.SITE_SUSPEND,
                        new SiteResourceRef(siteKey), new SiteScope(siteKey)),
                snapshot);
    }

    @Override
    public AccessDecision canArchiveSite(final Actor actor, final SiteKey siteKey,
                                         final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        LOGGER.debug("canArchiveSite: actor=[{}] site=[{}]", actor.id().value(), siteKey.value());
        return decisionService.evaluate(
                AccessDecisionRequest.of(actor, Permissions.SITE_ARCHIVE,
                        new SiteResourceRef(siteKey), new SiteScope(siteKey)),
                snapshot);
    }
}
