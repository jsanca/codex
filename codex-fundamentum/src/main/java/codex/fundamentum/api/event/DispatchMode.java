package codex.fundamentum.api.event;

/**
 * Indicates how a {@link CodexEvent} should be delivered by the dispatcher.
 *
 * <p>The mode can be declared at the event-type level by implementing {@link AsyncCodexEvent},
 * or overridden per call-site by wrapping the event in an {@link EventEnvelope}.</p>
 *
 * @author jsanca
 */
public enum DispatchMode {

    /** Deliver the event inline, on the calling thread. */
    SYNC,

    /** Deliver the event on a separate virtual thread, without blocking the caller. */
    ASYNC
}
