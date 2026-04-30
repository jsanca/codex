package codex.codex.internal.service;

import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.event.AsyncCodexEvent;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.tx.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeferredEventDispatcherTest {

    private RecordingEventDispatcher delegate;
    private CapturingExecutor asyncExecutor;
    private DeferredEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        delegate = new RecordingEventDispatcher();
        asyncExecutor = new CapturingExecutor();
        dispatcher = new DeferredEventDispatcher(delegate, asyncExecutor);
    }

    // --- Outside a transaction ---

    @Test
    void dispatch_outsideTransaction_syncEvent_delegateCalledImmediately() {
        SyncEvent event = new SyncEvent();

        dispatcher.dispatch(event);

        assertEquals(1, delegate.dispatched.size());
        assertSame(event, delegate.dispatched.getFirst());
        assertTrue(asyncExecutor.submitted.isEmpty());
    }

    @Test
    void dispatch_outsideTransaction_asyncEvent_submittedToExecutorImmediately() {
        AsyncEvent event = new AsyncEvent();

        dispatcher.dispatch(event);

        assertTrue(delegate.dispatched.isEmpty());
        assertEquals(1, asyncExecutor.submitted.size());
    }

    // --- Inside a transaction — commit path ---

    @Test
    void dispatch_insideTransaction_syncEvent_accumulatedAndDispatchedOnCommit() throws Exception {
        SyncEvent event = new SyncEvent();

        TransactionContext.runInTransaction(() -> {
            dispatcher.dispatch(event);
            assertTrue(delegate.dispatched.isEmpty(), "must not dispatch before commit");
            return null;
        });

        assertEquals(1, delegate.dispatched.size());
        assertSame(event, delegate.dispatched.getFirst());
        assertTrue(asyncExecutor.submitted.isEmpty());
    }

    @Test
    void dispatch_insideTransaction_asyncEvent_accumulatedAndSubmittedOnCommit() throws Exception {
        AsyncEvent event = new AsyncEvent();

        TransactionContext.runInTransaction(() -> {
            dispatcher.dispatch(event);
            assertTrue(asyncExecutor.submitted.isEmpty(), "must not submit before commit");
            return null;
        });

        assertTrue(delegate.dispatched.isEmpty());
        assertEquals(1, asyncExecutor.submitted.size());
    }

    @Test
    void dispatch_insideTransaction_multipleEvents_allDispatchedInOrderOnCommit() throws Exception {
        SyncEvent first = new SyncEvent();
        SyncEvent second = new SyncEvent();
        SyncEvent third = new SyncEvent();

        TransactionContext.runInTransaction(() -> {
            dispatcher.dispatch(first);
            dispatcher.dispatch(second);
            dispatcher.dispatch(third);
            return null;
        });

        assertEquals(List.of(first, second, third), delegate.dispatched);
    }

    // --- Inside a transaction — rollback path ---

    @Test
    void dispatch_insideTransaction_syncEvent_discardedOnRollback() {
        SyncEvent event = new SyncEvent();

        assertThrows(RuntimeException.class, () ->
            TransactionContext.runInTransaction(() -> {
                dispatcher.dispatch(event);
                throw new RuntimeException("forced rollback");
            })
        );

        assertTrue(delegate.dispatched.isEmpty());
        assertTrue(asyncExecutor.submitted.isEmpty());
    }

    @Test
    void dispatch_insideTransaction_asyncEvent_discardedOnRollback() {
        AsyncEvent event = new AsyncEvent();

        assertThrows(RuntimeException.class, () ->
            TransactionContext.runInTransaction(() -> {
                dispatcher.dispatch(event);
                throw new RuntimeException("forced rollback");
            })
        );

        assertTrue(delegate.dispatched.isEmpty());
        assertTrue(asyncExecutor.submitted.isEmpty());
    }

    @Test
    void dispatch_insideTransaction_multipleEvents_allDiscardedOnRollback() {
        assertThrows(RuntimeException.class, () ->
            TransactionContext.runInTransaction(() -> {
                dispatcher.dispatch(new SyncEvent());
                dispatcher.dispatch(new AsyncEvent());
                dispatcher.dispatch(new SyncEvent());
                throw new RuntimeException("forced rollback");
            })
        );

        assertTrue(delegate.dispatched.isEmpty());
        assertTrue(asyncExecutor.submitted.isEmpty());
    }

    // --- Test helpers ---

    private static final class RecordingEventDispatcher implements CodexEventDispatcher {

        final List<CodexEvent> dispatched = new ArrayList<>();

        @Override
        public void dispatch(final CodexEvent event) {
            dispatched.add(event);
        }
    }

    private static final class CapturingExecutor implements CodexExecutor {

        final List<Runnable> submitted = new ArrayList<>();

        @Override
        public void submit(final Runnable task) {
            submitted.add(task);
        }

        @Override
        public void shutdown() {
        }
    }

    private static final class SyncEvent implements CodexEvent {

        @Override
        public Instant occurredAt() {
            return Instant.now();
        }
    }

    private static final class AsyncEvent implements AsyncCodexEvent {

        @Override
        public Instant occurredAt() {
            return Instant.now();
        }
    }
}
