package codex.fundamentum.api.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Wraps a {@link CodexEvent} with an explicit {@link DispatchMode}, overriding whatever
 * mode the event would carry by its own type.
 *
 * <p>Use {@link #async(CodexEvent)} to force asynchronous delivery for an event that is
 * synchronous by nature, or {@link #sync(CodexEvent)} to force synchronous delivery for
 * an event that implements {@link AsyncCodexEvent}.</p>
 *
 * <p>The envelope implements {@link CodexEvent} so the {@code dispatch} signature is
 * unchanged. Dispatchers are responsible for unwrapping it.</p>
 *
 * @param event the wrapped event; never null
 * @param mode  the delivery mode to apply; never null
 * @author jsanca
 */
public record EventEnvelope(CodexEvent event, DispatchMode mode) implements CodexEvent {

    public EventEnvelope {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
    }

    /** Wraps {@code event} for asynchronous delivery regardless of its own type. */
    public static EventEnvelope async(final CodexEvent event) {
        return new EventEnvelope(event, DispatchMode.ASYNC);
    }

    /** Wraps {@code event} for synchronous delivery regardless of its own type. */
    public static EventEnvelope sync(final CodexEvent event) {
        return new EventEnvelope(event, DispatchMode.SYNC);
    }

    @Override
    public Instant occurredAt() {
        return event.occurredAt();
    }

    @Override
    public String eventType() {
        return event.eventType();
    }
}
