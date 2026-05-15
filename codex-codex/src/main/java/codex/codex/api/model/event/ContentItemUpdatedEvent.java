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
 * Dispatched when an existing {@link codex.codex.api.model.entity.ContentItem} is updated
 * (e.g., field values or metadata on its current working revision are modified).
 * <p>
 * Content values are intentionally excluded from this event. They may be large or sensitive;
 * downstream subscribers can load the item by {@link #id()} if they need the full payload.
 *
 * @param id                   the content item identifier
 * @param siteKey              the site scope under which the item was updated
 * @param contentTypeKey       the content type key the item belongs to
 * @param contentTypeVersionId the schema version the item was validated against
 * @param key                  the content item key
 * @param actor                the principal who triggered the update
 * @param occurredAt           the instant at which the event occurred
 */
public record ContentItemUpdatedEvent(
        ContentItemId id,
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentTypeVersionId contentTypeVersionId,
        ContentItemKey key,
        Actor actor,
        Instant occurredAt
) implements CodexEvent {

    public ContentItemUpdatedEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentTypeVersionId, "contentTypeVersionId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
