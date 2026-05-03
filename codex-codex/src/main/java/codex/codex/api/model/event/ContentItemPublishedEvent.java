package codex.codex.api.model.event;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.model.Actor;

import java.time.Instant;
import java.util.Objects;

/**
 * Dispatched when a {@link codex.codex.api.model.entity.ContentItem} transitions to
 * {@code PUBLISHED} status for the first time (or re-publishes a new revision).
 * <p>
 * Content values are intentionally excluded. They may be large or sensitive.
 * Downstream subscribers can resolve the canonical revision by loading {@link #publishedRevisionId()}.
 * <p>
 * This event is the future trigger for:
 * <ul>
 *   <li>search indexing</li>
 *   <li>cache invalidation</li>
 *   <li>audit</li>
 *   <li>workflow continuation</li>
 *   <li>external publication</li>
 * </ul>
 * Those concerns are not implemented in this task.
 * <p>
 * Idempotent publishes — where the item was already published with the same revision — do not
 * produce a new event.
 *
 * @param id                   the content item identifier
 * @param siteKey              the site scope
 * @param contentTypeKey       the content type key
 * @param contentTypeVersionId the schema version the item was validated against
 * @param key                  the content item key
 * @param publishedRevisionId  the revision that was transitioned to {@code PUBLISHED} status;
 *                             use this to resolve values when needed
 * @param actor                the principal who triggered the publish
 * @param occurredAt           the instant at which the event occurred
 */
public record ContentItemPublishedEvent(
        ContentItemId id,
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentTypeVersionId contentTypeVersionId,
        ContentItemKey key,
        ContentRevisionId publishedRevisionId,
        Actor actor,
        Instant occurredAt
) implements CodexEvent {

    public ContentItemPublishedEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentTypeVersionId, "contentTypeVersionId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(publishedRevisionId, "publishedRevisionId must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
