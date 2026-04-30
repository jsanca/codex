package codex.codex.internal.service;

import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.event.AsyncCodexEvent;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.event.DispatchMode;
import codex.fundamentum.api.event.EventEnvelope;
import codex.fundamentum.api.tx.TransactionCallback;
import codex.fundamentum.api.tx.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link CodexEventDispatcher} that defers event delivery until the enclosing transaction
 * commits, and respects the {@link DispatchMode} of each event.
 *
 * <p>Dispatch mode resolution (envelope takes precedence):</p>
 * <ol>
 *   <li>{@link EventEnvelope} — uses the envelope's explicit mode.</li>
 *   <li>{@link AsyncCodexEvent} — uses {@link DispatchMode#ASYNC}.</li>
 *   <li>Otherwise — uses {@link DispatchMode#SYNC}.</li>
 * </ol>
 *
 * <p>Transactional behaviour:</p>
 * <ul>
 *   <li>Inside a transaction — events accumulate in a per-transaction buffer.
 *       On commit the buffer is flushed respecting each event's mode; on rollback it is
 *       discarded.</li>
 *   <li>Outside a transaction — events are delivered immediately respecting their mode.</li>
 * </ul>
 *
 * @author jsanca
 */
public final class DeferredEventDispatcher implements CodexEventDispatcher {

    private final CodexEventDispatcher delegate;
    private final CodexExecutor asyncExecutor;

    /**
     * @param delegate      the real dispatcher that performs actual event delivery; must not be null
     * @param asyncExecutor executor used for {@link DispatchMode#ASYNC} events; must not be null
     */
    public DeferredEventDispatcher(final CodexEventDispatcher delegate,
                                   final CodexExecutor asyncExecutor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor must not be null");
    }

    @Override
    public void dispatch(final CodexEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        final DispatchMode mode = resolveMode(event);
        final CodexEvent unwrapped = unwrap(event);

        if (TransactionContext.isActive()) {
            pendingBuffer().add(unwrapped, mode);
        } else {
            doDispatch(unwrapped, mode);
        }
    }

    private EventBuffer pendingBuffer() {
        final TransactionContext ctx = TransactionContext.current();
        return ctx.computeIfAbsent(EventBuffer.class, () -> {
            final EventBuffer buffer = new EventBuffer(delegate, asyncExecutor);
            ctx.registerCallback(buffer);
            return buffer;
        });
    }

    private static DispatchMode resolveMode(final CodexEvent event) {
        if (event instanceof EventEnvelope envelope) {
            return envelope.mode();
        }
        if (event instanceof AsyncCodexEvent) {
            return DispatchMode.ASYNC;
        }
        return DispatchMode.SYNC;
    }

    private static CodexEvent unwrap(final CodexEvent event) {
        return event instanceof EventEnvelope envelope ? envelope.event() : event;
    }

    private void doDispatch(final CodexEvent event, final DispatchMode mode) {
        if (mode == DispatchMode.ASYNC) {
            asyncExecutor.submit(() -> delegate.dispatch(event));
        } else {
            delegate.dispatch(event);
        }
    }

    /**
     * Per-transaction accumulator. Registered as a {@link TransactionCallback} so it can
     * flush or discard pending events based on the transaction outcome.
     */
    private static final class EventBuffer implements TransactionCallback {

        private record PendingDispatch(CodexEvent event, DispatchMode mode) {}

        private final CodexEventDispatcher delegate;
        private final CodexExecutor asyncExecutor;
        private final List<PendingDispatch> pending = new ArrayList<>();

        private EventBuffer(final CodexEventDispatcher delegate, final CodexExecutor asyncExecutor) {
            this.delegate = delegate;
            this.asyncExecutor = asyncExecutor;
        }

        void add(final CodexEvent event, final DispatchMode mode) {
            pending.add(new PendingDispatch(event, mode));
        }

        @Override
        public void onCommit() {
            pending.forEach(pd -> {
                if (pd.mode() == DispatchMode.ASYNC) {
                    asyncExecutor.submit(() -> delegate.dispatch(pd.event()));
                } else {
                    delegate.dispatch(pd.event());
                }
            });
        }

        @Override
        public void onRollback() {
            pending.clear();
        }
    }
}
