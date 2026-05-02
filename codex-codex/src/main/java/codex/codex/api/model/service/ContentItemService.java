package codex.codex.api.model.service;

import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for content item lifecycle operations.
 * <p>
 * Every operation receives an {@link Actor} for audit context. Permission enforcement
 * is not part of this interface and will be added in a later task.
 */
public interface ContentItemService {

    /**
     * Creates a new content item in {@code DRAFT} status.
     * <p>
     * Validates the command values against the latest published {@code ContentTypeVersion}.
     * Throws if the content type is not active, has no published version, or if unknown
     * or missing required fields are detected.
     *
     * @param command the creation command; must not be null
     * @param actor   the acting user; must not be null
     * @return the newly created content item
     */
    ContentItem create(CreateContentItemCommand command, Actor actor);

    /**
     * Finds a content item by its scoped identity.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     * @param actor          the acting user; must not be null
     * @return the content item wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentItem> findByKey(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key, Actor actor);

    /**
     * Returns all content items for a given site and content type.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param actor          the acting user; must not be null
     * @return immutable list of matching content items; never null
     */
    List<ContentItem> findByContentType(SiteKey siteKey, ContentTypeKey contentTypeKey, Actor actor);

    /**
     * Returns all content items across all sites and content types.
     *
     * @param actor the acting user; must not be null
     * @return immutable list of all content items; never null
     */
    List<ContentItem> findAll(Actor actor);
}
