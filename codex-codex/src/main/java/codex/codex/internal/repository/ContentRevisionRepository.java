package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentRevisionId;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for {@link ContentRevision} entities.
 * <p>
 * Revisions are append-only snapshots. Implementations own only storage concerns —
 * no business logic belongs here. There is intentionally no delete or update operation.
 */
public interface ContentRevisionRepository {

    /**
     * Persists a content revision.
     *
     * @param revision the revision to save; must not be null
     * @return the saved revision
     */
    ContentRevision save(ContentRevision revision);

    /**
     * Finds a revision by its unique identifier.
     *
     * @param id the revision id; must not be null
     * @return the revision wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentRevision> findById(ContentRevisionId id);

    /**
     * Finds a specific revision number for a given content item.
     *
     * @param contentItemId  the parent content item id; must not be null
     * @param revisionNumber the revision number; must be >= 1
     * @return the revision wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentRevision> findByContentItemAndRevision(ContentItemId contentItemId, int revisionNumber);

    /**
     * Returns the latest {@code WORKING} revision for a given content item.
     * If multiple revisions have status {@code WORKING}, the one with the highest
     * revision number is returned.
     *
     * @param contentItemId the content item id; must not be null
     * @return the latest working revision wrapped in an {@link Optional}, or empty if none
     */
    Optional<ContentRevision> findLatestWorking(ContentItemId contentItemId);

    /**
     * Returns the latest {@code PUBLISHED} revision for a given content item.
     * If multiple revisions have status {@code PUBLISHED}, the one with the highest
     * revision number is returned.
     *
     * @param contentItemId the content item id; must not be null
     * @return the latest published revision wrapped in an {@link Optional}, or empty if none
     */
    Optional<ContentRevision> findLatestPublished(ContentItemId contentItemId);

    /**
     * Returns all revisions for a given content item, sorted by revision number ascending.
     *
     * @param contentItemId the content item id; must not be null
     * @return immutable list of all revisions for the content item; never null
     */
    List<ContentRevision> findByContentItem(ContentItemId contentItemId);
}
