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
 * Dispatched when a {@link codex.codex.api.model.entity.ContentType} transitions to {@code ACTIVE} status.
 * Not dispatched for idempotent activations (already {@code ACTIVE}).
 *
 * @param id             the content type identifier
 * @param siteKey        the site scope of the content type
 * @param key            the content type key
 * @param previousStatus the status before activation
 * @param newStatus      the status after activation (always {@code ACTIVE})
 * @param actor          the principal who triggered the activation
 * @param occurredAt     the instant at which the event occurred
 */
public record ContentTypeActivatedEvent(
        ContentTypeId id,
        SiteKey siteKey,
        ContentTypeKey key,
        ContentTypeStatus previousStatus,
        ContentTypeStatus newStatus,
        Actor actor,
        Instant occurredAt
) implements CodexEvent {

    public ContentTypeActivatedEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(previousStatus, "previousStatus must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
