package codex.fundamentum.api.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents the identity of an actor (e.g., a user or system process) within Codex.
 * This is a lightweight reference to avoid coupling the core content model to the full identity system.
 */
public record ActorId(String value) {
    public ActorId {
        Objects.requireNonNull(value, "ActorId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ActorId value cannot be empty");
        }
    }

    public static ActorId of(String value) {
        return new ActorId(value);
    }

}
