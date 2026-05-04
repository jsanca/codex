package codex.codex.api.projection;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Optional;

/**
 * Narrow read/projection contract for modules that need canonical content state.
 *
 * <p>Projection modules such as {@code codex-index} and {@code codex-chronicon} depend on this
 * interface instead of reaching into {@code codex-codex} internal repositories. This keeps
 * the boundary clean: the core exposes only what projection consumers need.</p>
 *
 * <p>No write methods. No generic query DSL. No cache or transaction semantics.
 * Implementations are free to add those concerns behind this contract.</p>
 */
public interface ContentItemProjectionReader {

    /**
     * Finds a content item by its scoped identity.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     * @return the content item wrapped in an {@link Optional}, or empty if not found
     * @throws NullPointerException if any argument is null
     */
    Optional<ContentItem> findContentItem(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key);

    /**
     * Finds a content revision by its unique identifier.
     *
     * @param revisionId the revision identifier; must not be null
     * @return the revision wrapped in an {@link Optional}, or empty if not found
     * @throws NullPointerException if {@code revisionId} is null
     */
    Optional<ContentRevision> findContentRevision(ContentRevisionId revisionId);
}
