package codex.codex.api.model.event;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.model.Actor;

import java.time.Instant;
import java.util.Objects;

/**
 * Dispatched when a {@link codex.codex.api.model.entity.ContentItem} transitions to
 * {@code ARCHIVED} status.
 * <p>
 * Content values are intentionally excluded from this event. They may be large or sensitive;
 * downstream subscribers can load the item by {@link #id()} if they need the full payload.
 *
 * @param id                   the content item identifier
 * @param siteKey              the site scope under which the item was archived
 * @param contentTypeKey       the content type key the item belongs to
 * @param contentTypeVersionId the schema version the item was last validated against
 * @param key                  the content item key
 * @param actor                the principal who triggered the archive
 * @param occurredAt           the instant at which the event occurred
 */
public record ContentItemArchivedEvent(
        ContentItemId id,
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentTypeVersionId contentTypeVersionId,
        ContentItemKey key,
        Actor actor,
        Instant occurredAt
) implements CodexEvent {

    public ContentItemArchivedEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentTypeVersionId, "contentTypeVersionId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
