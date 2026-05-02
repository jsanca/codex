package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentTypeVersion;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeVersionId;

import java.util.List;
import java.util.Optional;

/**
 * Repository contract for {@link ContentTypeVersion} persistence.
 * <p>
 * Implementations are CRUD-only — no business logic belongs here.
 * Key lookups are by {@link ContentTypeVersionId}, {@link ContentTypeId}, or
 * content type + version number combinations.
 */
public interface ContentTypeVersionRepository {

    /**
     * Saves a content type version, creating or replacing it.
     *
     * @param version the version to save; must not be null
     * @return the saved version
     */
    ContentTypeVersion save(ContentTypeVersion version);

    /**
     * Finds a version by its unique identifier.
     *
     * @param id the version id; must not be null
     * @return the version wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentTypeVersion> findById(ContentTypeVersionId id);

    /**
     * Finds a specific version number for a given content type.
     *
     * @param contentTypeId the parent content type id; must not be null
     * @param version       the version number; must be >= 1
     * @return the version wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentTypeVersion> findByContentTypeAndVersion(ContentTypeId contentTypeId, int version);

    /**
     * Returns the latest published version for a given content type.
     * If multiple versions have status {@code PUBLISHED}, the one with the highest
     * version number is returned.
     *
     * @param contentTypeId the parent content type id; must not be null
     * @return the latest published version wrapped in an {@link Optional}, or empty if none
     */
    Optional<ContentTypeVersion> findLatestPublished(ContentTypeId contentTypeId);

    /**
     * Returns all versions for a given content type, in no guaranteed order.
     *
     * @param contentTypeId the parent content type id; must not be null
     * @return immutable list of all versions for the content type
     */
    List<ContentTypeVersion> findByContentType(ContentTypeId contentTypeId);
}
