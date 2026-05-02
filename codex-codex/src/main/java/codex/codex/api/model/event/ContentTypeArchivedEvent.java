package codex.codex.api.model.event;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.model.Actor;

import java.time.Instant;
import java.util.Objects;

/**
 * Dispatched when a {@link codex.codex.api.model.entity.ContentType} transitions to {@code ARCHIVED} status.
 * Not dispatched for idempotent archives (already {@code ARCHIVED}).
 *
 * @param id             the content type identifier
 * @param siteKey        the site scope of the content type
 * @param key            the content type key
 * @param previousStatus the status before archiving
 * @param newStatus      the status after archiving (always {@code ARCHIVED})
 * @param actor          the principal who triggered the archive
 * @param occurredAt     the instant at which the event occurred
 */
public record ContentTypeArchivedEvent(
        ContentTypeId id,
        SiteKey siteKey,
        ContentTypeKey key,
        ContentTypeStatus previousStatus,
        ContentTypeStatus newStatus,
        Actor actor,
        Instant occurredAt
) implements CodexEvent {

    public ContentTypeArchivedEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(previousStatus, "previousStatus must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
