package codex.codex.api.model.event;

import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.model.Actor;

import java.time.Instant;

public record SiteCreatedEvent(SiteId id, SiteKey key, Actor actor, Instant instant) implements CodexEvent {

    @Override
    public Instant occurredAt() {
        return instant;
    }
}
