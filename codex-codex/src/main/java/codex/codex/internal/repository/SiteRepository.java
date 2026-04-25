package codex.codex.internal.repository;

import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Site} entities.
 * <p>
 * This interface defines the contract for persisting and retrieving site information,
 * abstracting away the underlying persistence mechanism (e.g., in-memory, JDBC, JPA).
 * Implementations should adhere to domain-driven principles, focusing solely on
 * CRUD operations for the {@link Site} aggregate root.
 *
 * @see codex.codex.api.model.entity.Site
 *
 */
public interface SiteRepository {

    /**
     * Saves or updates a site.
     * @param site The site to save. Must not be null.
     * @return The saved site.
     */
    Site save(Site site);

    /**
     * Finds a site by its stable key.
     * @param siteKey the stable site key to look up, must not be null
     * @return An Optional containing the site if found.
     */
    Optional<Site> findByKey(SiteKey siteKey);

    /**
     * Finds a site by its alias.
     *
     * @param alias the alias to look up, must not be null
     * @return An Optional containing the site if found.
     */
    Optional<Site> findByAlias(SiteAlias alias);

    /**
     * Checks if a site with the given key already exists.
     * @param siteKey the stable site key to check, must not be null
     * @return true if the site exists, false otherwise.
     */
    boolean existsByKey(SiteKey siteKey);

    /**
     * Retrieves all currently stored sites.
     * @return A list of all sites.
     */
    List<Site> findAll();

    /**
     * Deletes a site by its stable key.
     *
     * @param siteKey the stable site key to delete, must not be null
     * @return true if the site was deleted, false otherwise
     */
    boolean deleteByKey(SiteKey siteKey);
}
