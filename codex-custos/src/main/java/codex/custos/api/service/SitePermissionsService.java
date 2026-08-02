package codex.custos.api.service;

import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.fundamentum.api.model.Actor;

/**
 * Domain-specific authorization service for site operations.
 * <p>
 * Translates site operation intent into an {@link AccessDecision} by mapping each operation
 * to the correct {@link codex.custos.api.model.PermissionKey}, {@link codex.custos.api.model.ResourceRef},
 * and {@link codex.custos.api.model.ResourceScope}, then delegating to {@link AccessDecisionService}.
 * <p>
 * Absent from this service (not in the Permissions catalog):
 * {@code site.update} and {@code site.delete}. Add the corresponding {@code PermissionKey}
 * constants to {@link codex.custos.api.model.Permissions} before introducing those methods.
 */
public interface SitePermissionsService {

    /**
     * Returns whether {@code actor} may create a new site.
     * <p>
     * Site creation is a global operation — no site exists yet — so the resource and scope
     * are {@link codex.custos.api.model.GlobalResourceRef} / {@link codex.custos.api.model.GlobalScope}.
     *
     * @param actor    the actor requesting the operation; must not be null
     * @param snapshot role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canCreateSite(Actor actor, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may read the metadata of the specified site.
     *
     * @param actor    the actor requesting the operation; must not be null
     * @param siteKey  the site to check; must not be null
     * @param snapshot role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canReadSite(Actor actor, SiteKey siteKey, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may transition the specified site from SUSPENDED to STARTED.
     *
     * @param actor    the actor requesting the operation; must not be null
     * @param siteKey  the site to check; must not be null
     * @param snapshot role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canStartSite(Actor actor, SiteKey siteKey, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may transition the specified site from STARTED to SUSPENDED.
     *
     * @param actor    the actor requesting the operation; must not be null
     * @param siteKey  the site to check; must not be null
     * @param snapshot role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canSuspendSite(Actor actor, SiteKey siteKey, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may transition the specified site to ARCHIVED.
     *
     * @param actor    the actor requesting the operation; must not be null
     * @param siteKey  the site to check; must not be null
     * @param snapshot role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canArchiveSite(Actor actor, SiteKey siteKey, PermissionResolutionSnapshot snapshot);
}
