package codex.fundamentum.api.event;

/**
 * Marker interface for events that are asynchronous by nature.
 *
 * <p>When a dispatcher receives an event that implements this interface it will deliver it
 * on a separate virtual thread without blocking the caller. This default can be overridden
 * for a specific call-site by wrapping the event in an {@link EventEnvelope}.</p>
 *
 * @author jsanca
 */
public interface AsyncCodexEvent extends CodexEvent {
}
