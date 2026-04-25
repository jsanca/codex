package codex.fundamentum.api.event;

import java.time.Instant;

/**
 * Represents a domain or application event emitted by Codex.
 *
 * <p>A Codex event describes something that already happened in the system.
 * Events are immutable facts and should not contain execution logic.</p>
 */
public interface CodexEvent {

    /**
     * Returns the instant when the event occurred.
     *
     * @return The event occurrence timestamp.
     */
    Instant occurredAt();

    /**
     * Returns the logical event type.
     *
     * <p>By default, the simple class name is used. Event implementations may
     * override this method if they need a stable external name for serialization,
     * integrations, or event storage.</p>
     *
     * @return The event type.
     */
    default String eventType() {
        return getClass().getSimpleName();
    }
}
