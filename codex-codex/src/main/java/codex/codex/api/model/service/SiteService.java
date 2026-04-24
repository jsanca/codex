package codex.codex.api.model.service;

import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;

import java.util.List;
import java.util.Optional;

// ... existing code ...
/**
 * Service interface for managing {@link codex.codex.api.model.entity.Site} entities.
 * Provides the primary entry point for site-related business operations.
 * @author jsanca
 */
public interface SiteService {

    /**
     * Creates and persists a new site instance associated with the given actor.
     *
     * @param site The site entity to be created. Must not be null.
     * @param actor The actor initiating the creation. Must not be null.
     * @return The saved, fully constructed Site entity, including generated IDs or keys.
     * @throws IllegalArgumentException if the provided site or actor is null.
     */
    Site create(Site site, Actor actor);

    /**
     * Retrieves an existing site using its unique, stable key.
     *
     * @param siteKey The stable key identifying the site. Must not be null.
     * @param actor The actor requesting the information (for auditing/context). Must not be null.
     * @return An {@code Optional} containing the Site if it exists, otherwise {@code Optional.empty()}.
     * @throws IllegalArgumentException if the provided siteKey or actor is null.
     */
    Optional<Site> findByKey(SiteKey siteKey, Actor actor);

    /**
     * Start an existing site
     *
     * @param siteKey The stable key of the site to suspend. Must not be null.
     * @param actor The actor performing the suspension. Must not be null.
     * @return The updated Site entity in started state.
     * @throws IllegalArgumentException if the provided siteKey or actor is null.
     */
    Site start(SiteKey siteKey, Actor actor);

    /**
     * Suspends an existing site, making it temporarily inactive.
     *
     * @param siteKey The stable key of the site to suspend. Must not be null.
     * @param actor The actor performing the suspension. Must not be null.
     * @return The updated Site entity in suspended state.
     * @throws IllegalArgumentException if the provided siteKey or actor is null.
     */
    Site suspend(SiteKey siteKey, Actor actor);

    /**
     * Archives an existing site, moving it to a long-term storage/inactive state.
     *
     * @param siteKey The stable key of the site to archive. Must not be null.
     * @param actor The actor performing the archiving. Must not be null.
     * @return The updated Site entity in archived state.
     * @throws IllegalArgumentException if the provided siteKey or actor is null.
     */
    Site archive(SiteKey siteKey, Actor actor);

    /**
     * Finds a site based on a specific alias.
     *
     * @param alias The alias to search for. Must not be null.
     * @param actor The actor requesting the information. Must not be null.
     * @return An {@code Optional} containing the Site if found, otherwise {@code Optional.empty()}.
     * @throws IllegalArgumentException if the provided alias or actor is null.
     */
    Optional<Site> findByAlias(SiteAlias alias, Actor actor);

    /**
     * Retrieves all sites accessible to the given actor.
     *
     * @param actor The actor requesting the list. Must not be null.
     * @return A list of {@link Site} entities.
     * @throws IllegalArgumentException if the actor is null.
     */
    List<Site> findAll(Actor actor);
}