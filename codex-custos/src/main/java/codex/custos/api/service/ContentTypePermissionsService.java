package codex.custos.api.service;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.fundamentum.api.model.Actor;

/**
 * Domain-specific authorization service for content type operations.
 * <p>
 * Translates content type operation intent into an {@link AccessDecision} by mapping each
 * operation to the correct {@link codex.custos.api.model.PermissionKey},
 * {@link codex.custos.api.model.ResourceRef}, and {@link codex.custos.api.model.ResourceScope},
 * then delegating to {@link AccessDecisionService}.
 */
public interface ContentTypePermissionsService {

    /**
     * Returns whether {@code actor} may create a new content type within the specified site.
     * <p>
     * Creation is checked at site scope — no content type exists yet — so the resource and
     * scope are {@link codex.custos.api.model.SiteResourceRef} / {@link codex.custos.api.model.SiteScope}.
     *
     * @param actor    the actor requesting the operation; must not be null
     * @param siteKey  the site in which the content type would be created; must not be null
     * @param snapshot role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canCreateContentType(Actor actor, SiteKey siteKey, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may read the schema and metadata of the specified content type.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site containing the content type; must not be null
     * @param contentTypeKey the content type to check; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canReadContentType(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                      PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may modify the schema of the specified content type.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site containing the content type; must not be null
     * @param contentTypeKey the content type to check; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canUpdateContentType(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                        PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may archive the specified content type,
     * removing it from active use.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site containing the content type; must not be null
     * @param contentTypeKey the content type to check; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canArchiveContentType(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                         PermissionResolutionSnapshot snapshot);
}
