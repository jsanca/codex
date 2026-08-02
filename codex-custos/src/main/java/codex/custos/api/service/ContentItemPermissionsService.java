package codex.custos.api.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.fundamentum.api.model.Actor;

/**
 * Domain-specific authorization service for content item operations.
 * <p>
 * Translates content item operation intent into an {@link AccessDecision} by mapping each
 * operation to the correct {@link codex.custos.api.model.PermissionKey},
 * {@link codex.custos.api.model.ResourceRef}, and {@link codex.custos.api.model.ResourceScope},
 * then delegating to {@link AccessDecisionService}.
 * <p>
 * Absent from this service (not in the Permissions catalog): {@code contentItem.delete}.
 * Add the corresponding {@code PermissionKey} constant to
 * {@link codex.custos.api.model.Permissions} before introducing {@code canDeleteContentItem}.
 */
public interface ContentItemPermissionsService {

    /**
     * Returns whether {@code actor} may create a new content item of the specified type.
     * <p>
     * Creation is checked at content-type scope — no item exists yet — so the resource and
     * scope are {@link codex.custos.api.model.ContentTypeResourceRef} /
     * {@link codex.custos.api.model.ContentTypeScope}.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site in which the item would be created; must not be null
     * @param contentTypeKey the content type the item would belong to; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canCreateContentItem(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                        PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may read the specified content item.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site containing the item; must not be null
     * @param contentTypeKey the content type of the item; must not be null
     * @param contentItemKey the item to check; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canReadContentItem(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                      ContentItemKey contentItemKey, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may modify the body or metadata of the specified content item.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site containing the item; must not be null
     * @param contentTypeKey the content type of the item; must not be null
     * @param contentItemKey the item to check; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canUpdateContentItem(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                        ContentItemKey contentItemKey, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may publish the specified content item.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site containing the item; must not be null
     * @param contentTypeKey the content type of the item; must not be null
     * @param contentItemKey the item to check; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canPublishContentItem(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                         ContentItemKey contentItemKey, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may unpublish the specified content item.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site containing the item; must not be null
     * @param contentTypeKey the content type of the item; must not be null
     * @param contentItemKey the item to check; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canUnpublishContentItem(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                           ContentItemKey contentItemKey, PermissionResolutionSnapshot snapshot);

    /**
     * Returns whether {@code actor} may archive the specified content item.
     *
     * @param actor          the actor requesting the operation; must not be null
     * @param siteKey        the site containing the item; must not be null
     * @param contentTypeKey the content type of the item; must not be null
     * @param contentItemKey the item to check; must not be null
     * @param snapshot       role assignments and role blueprints in effect; must not be null
     * @return an {@link AccessDecision.Granted} or {@link AccessDecision.Denied}
     */
    AccessDecision canArchiveContentItem(Actor actor, SiteKey siteKey, ContentTypeKey contentTypeKey,
                                         ContentItemKey contentItemKey, PermissionResolutionSnapshot snapshot);
}
