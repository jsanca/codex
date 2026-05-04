package codex.codex.internal.index;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;

/**
 * Read facade used by indexing projection subscribers to load canonical data
 * required for building an {@link codex.codex.api.index.IndexDocument}.
 * <p>
 * Subscribers should depend on this interface, not directly on repositories. This allows
 * the underlying loading mechanism to evolve independently:
 * <ul>
 *   <li>current MVP: repository-backed ({@link RepositoryContentItemProjectionSource})</li>
 *   <li>future: cache-aware, read-only unit-of-work, read model, projection store</li>
 * </ul>
 * <p>
 * This source has no indexing, mapping, caching, or transaction behavior.
 * It only provides the canonical objects needed to build a projection document.
 * <p>
 * Audit, observability, cache invalidation, and workflow projections should use
 * their own dedicated sources, keeping each subscriber small and focused.
 */
public interface ContentItemProjectionSource {

    /**
     * Loads the {@link ContentItem} identified by the published event.
     *
     * @param event the {@link ContentItemPublishedEvent} that triggered this projection; must not be null
     * @return the canonical content item
     * @throws NullPointerException  if {@code event} is null
     * @throws IllegalStateException if no matching content item can be found
     */
    ContentItem loadItem(ContentItemPublishedEvent event);

    /**
     * Loads the published {@link ContentRevision} identified by the event's
     * {@link ContentItemPublishedEvent#publishedRevisionId()}.
     *
     * @param event the {@link ContentItemPublishedEvent} that triggered this projection; must not be null
     * @return the published revision
     * @throws NullPointerException  if {@code event} is null
     * @throws IllegalStateException if no matching revision can be found
     */
    ContentRevision loadPublishedRevision(ContentItemPublishedEvent event);
}
