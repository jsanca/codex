package codex.codex.api.model.event;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.model.Actor;

import java.time.Instant;
import java.util.Objects;

/**
 * Dispatched when a new {@link codex.codex.api.model.entity.ContentType} is created in {@code DRAFT} status.
 *
 * @param id          the content type identifier
 * @param siteKey     the site scope under which the content type was created
 * @param key         the content type key
 * @param actor       the principal who triggered the creation
 * @param occurredAt  the instant at which the event occurred
 */
public record ContentTypeCreatedEvent(
        ContentTypeId id,
        SiteKey siteKey,
        ContentTypeKey key,
        Actor actor,
        Instant occurredAt
) implements CodexEvent {

    public ContentTypeCreatedEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
