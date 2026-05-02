package codex.codex.api.model.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;

import java.util.List;
import java.util.Optional;

/**
 * Application service for managing {@link ContentType} lifecycle.
 * <p>
 * Content types are scoped by {@link SiteKey}. The logical identity is
 * {@code siteKey + contentTypeKey}. Every operation is actor-aware for audit context.
 * This interface is designed to be wrapped by event-publishing decorators following the
 * same pattern used by {@link SiteService}.
 */
public interface ContentTypeService {

    /**
     * Creates a new content type in {@code DRAFT} status.
     *
     * @param command the creation command containing site scope, key, and display name; must not be null
     * @param actor   the acting principal; must not be null
     * @return the persisted content type
     */
    ContentType create(CreateContentTypeCommand command, Actor actor);

    /**
     * Transitions a content type from {@code DRAFT} to {@code ACTIVE}.
     * Idempotent when already {@code ACTIVE}. Throws for invalid transitions.
     *
     * @param command the activation command; must not be null
     * @param actor   the acting principal; must not be null
     * @return the updated content type
     */
    ContentType activate(ActivateContentTypeCommand command, Actor actor);

    /**
     * Transitions a content type to {@code ARCHIVED} status.
     * Idempotent when already {@code ARCHIVED}.
     *
     * @param command the archive command; must not be null
     * @param actor   the acting principal; must not be null
     * @return the updated content type
     */
    ContentType archive(ArchiveContentTypeCommand command, Actor actor);

    /**
     * Finds a content type by its scoped identity.
     *
     * @param siteKey the site scope; must not be null
     * @param key     the content type key; must not be null
     * @param actor   the acting principal; must not be null
     * @return the content type wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentType> findByKey(SiteKey siteKey, ContentTypeKey key, Actor actor);

    /**
     * Returns all content types for a given site.
     *
     * @param siteKey the site scope; must not be null
     * @param actor   the acting principal; must not be null
     * @return immutable list of content types for the site
     */
    List<ContentType> findBySiteKey(SiteKey siteKey, Actor actor);

    /**
     * Returns all content types across all sites visible to the actor.
     *
     * @param actor the acting principal; must not be null
     * @return immutable list of all content types
     */
    List<ContentType> findAll(Actor actor);

    /**
     * Adds a field to the draft schema of an existing content type.
     * Only permitted while the content type is in {@code DRAFT} status.
     * Throws if the field key already exists.
     *
     * @param command the add-field command containing site scope, content type key, and field; must not be null
     * @param actor   the acting principal; must not be null
     * @return the updated content type with the new field included
     */
    ContentType addField(AddContentTypeFieldCommand command, Actor actor);

    /**
     * Removes a field by key from the draft schema of an existing content type.
     * Only permitted while the content type is in {@code DRAFT} status.
     * Throws if the field key does not exist.
     *
     * @param command the remove-field command containing site scope, content type key, and field key; must not be null
     * @param actor   the acting principal; must not be null
     * @return the updated content type with the field removed
     */
    ContentType removeField(RemoveContentTypeFieldCommand command, Actor actor);
}
