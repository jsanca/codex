package codex.codex.api.model.service;

import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
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

    /**
     * Updates the field values on the current working revision of a content item.
     * <p>
     * Validates the new values against the version recorded on the item. Throws if the
     * item does not exist or if unknown or missing required fields are detected.
     *
     * @param command the update command; must not be null
     * @param actor   the acting user; must not be null
     * @return the content item with updated metadata
     */
    ContentItem update(UpdateContentItemCommand command, Actor actor);

    /**
     * Archives a content item, transitioning it to {@code ARCHIVED} status.
     * <p>
     * Valid source states are {@code DRAFT} and {@code PUBLISHED}. If the item is
     * {@code PUBLISHED}, its publication state is cleared as part of the transition.
     * Throws if the item does not exist or is already {@code ARCHIVED}.
     *
     * @param command the archive command; must not be null
     * @param actor   the acting user; must not be null
     * @return the content item with updated status
     */
    ContentItem archive(ArchiveContentItemCommand command, Actor actor);

    /**
     * Unpublishes a content item, reverting it from {@code PUBLISHED} back to {@code DRAFT}.
     * <p>
     * Clears the item's {@code currentPublishedRevisionId} and reverts the formerly-published
     * revision to {@code WORKING} status. Throws if the item does not exist or is not currently
     * in {@code PUBLISHED} status.
     *
     * @param command the unpublish command; must not be null
     * @param actor   the acting user; must not be null
     * @return the content item with updated status
     */
    ContentItem unpublish(UnpublishContentItemCommand command, Actor actor);

    /**
     * Permanently deletes a content item and its identity from storage.
     * <p>
     * Only items in {@code ARCHIVED} status may be deleted. After deletion,
     * {@code findByKey} returns empty for the same identity.
     * Throws if the item does not exist or is not currently {@code ARCHIVED}.
     *
     * @param command the delete command; must not be null
     * @param actor   the acting user; must not be null
     */
    void delete(DeleteContentItemCommand command, Actor actor);

    /**
     * Restores a content item from {@code ARCHIVED} status back to {@code DRAFT}.
     * <p>
     * The item becomes editable again; it is not automatically republished.
     * {@code currentPublishedRevisionId} remains empty after the restore.
     * Throws if the item does not exist or is not currently {@code ARCHIVED}.
     *
     * @param command the restore command; must not be null
     * @param actor   the acting user; must not be null
     * @return the content item with {@code DRAFT} status
     */
    ContentItem restore(RestoreContentItemCommand command, Actor actor);

    /**
     * Publishes the current working revision of a content item.
     * <p>
     * Publishing is pointer-based: content values remain in {@link codex.codex.api.model.entity.ContentRevision}.
     * The working revision is transitioned to {@code PUBLISHED} status and the item's
     * {@code currentPublishedRevisionId} is updated to point to it.
     * <p>
     * For this first foundation, publishing sets both {@code currentWorkingRevisionId} and
     * {@code currentPublishedRevisionId} to the same revision. Future edit operations will
     * create a new {@code WORKING} revision that diverges from the published one.
     * <p>
     * Idempotent: publishing an already-published item whose pointers already match returns
     * the existing item without mutation.
     * <p>
     * Publish events and workflow are future work.
     * This operation should be executed inside a transaction boundary by a future wrapper.
     *
     * @param command the publish command; must not be null
     * @param actor   the acting user; must not be null
     * @return the updated content item with {@code PUBLISHED} status
     */
    ContentItem publish(PublishContentItemCommand command, Actor actor);
}
