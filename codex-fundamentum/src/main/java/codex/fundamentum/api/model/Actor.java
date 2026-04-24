package codex.fundamentum.api.model;

import java.util.Objects;

/**
 * Represents the entity currently performing an action in Codex.
 * <p>
 * An actor is an operational abstraction. It may represent a human user,
 * a system process, an AI agent, a scheduled job, or an external API client.
 * It intentionally avoids coupling Codex to any specific authentication
 * mechanism such as Spring Security, JWT, Auth0, or session-based security.
 */
public record Actor(
        ActorId id,
        String displayName,
        ActorType type
) {

    public Actor {
        Objects.requireNonNull(id, "Actor id cannot be null");
        Objects.requireNonNull(displayName, "Actor displayName cannot be null");
        Objects.requireNonNull(type, "Actor type cannot be null");

        displayName = displayName.trim();

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Actor displayName cannot be blank");
        }
    }

    public static Actor human(final ActorId id, final String displayName) {
        return new Actor(id, displayName, ActorType.HUMAN);
    }

    public static Actor system(final String name) {
        return new Actor(ActorId.of("system:" + name), name, ActorType.SYSTEM);
    }

    public static Actor agent(final String name) {
        return new Actor(ActorId.of("agent:" + name), name, ActorType.AGENT);
    }

    public static Actor apiClient(final String name) {
        return new Actor(ActorId.of("api-client:" + name), name, ActorType.API_CLIENT);
    }
}