package codex.codex.api.model.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.fundamentum.api.model.Actor;

import java.util.List;
import java.util.Optional;

/**
 * Application service for managing {@link ContentType} lifecycle.
 * <p>
 * Every operation is actor-aware for audit context. Implementations own business
 * semantics such as duplicate-key prevention, status transitions, and identity generation.
 * This interface is designed to be wrapped by event-publishing decorators following the
 * same pattern used by {@link SiteService}.
 */
public interface ContentTypeService {

    /**
     * Creates a new content type in {@code DRAFT} status.
     *
     * @param command the creation command; must not be null
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
     * Finds a content type by its stable key.
     *
     * @param key   the content type key; must not be null
     * @param actor the acting principal; must not be null
     * @return the content type wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentType> findByKey(ContentTypeKey key, Actor actor);

    /**
     * Returns all content types visible to the actor.
     *
     * @param actor the acting principal; must not be null
     * @return immutable list of all content types
     */
    List<ContentType> findAll(Actor actor);
}
