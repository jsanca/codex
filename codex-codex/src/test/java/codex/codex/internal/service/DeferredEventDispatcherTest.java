package codex.codex.internal.service;

import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.event.AsyncCodexEvent;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.observance.InMemoryObservance;
import codex.fundamentum.api.tx.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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

        @Override
        public void shutdownNow() {
        }

        @Override
        public boolean awaitTermination(final Duration timeout) {
            return true;
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

    // --- Observance tests ---

    @Test
    void dispatch_insideTransaction_incrementsBufferedCounter() throws Exception {
        final InMemoryObservance observance = new InMemoryObservance();
        final DeferredEventDispatcher obs = new DeferredEventDispatcher(delegate, asyncExecutor, observance);

        TransactionContext.runInTransaction(() -> {
            obs.dispatch(new SyncEvent());
            return null;
        });

        assertEquals(1, observance.counterValue("deferred.events.buffered.SyncEvent"));
    }

    @Test
    void dispatch_insideTransaction_multipleEvents_bufferedCounterReflectsEach() throws Exception {
        final InMemoryObservance observance = new InMemoryObservance();
        final DeferredEventDispatcher obs = new DeferredEventDispatcher(delegate, asyncExecutor, observance);

        TransactionContext.runInTransaction(() -> {
            obs.dispatch(new SyncEvent());
            obs.dispatch(new SyncEvent());
            obs.dispatch(new AsyncEvent());
            return null;
        });

        assertEquals(2, observance.counterValue("deferred.events.buffered.SyncEvent"));
        assertEquals(1, observance.counterValue("deferred.events.buffered.AsyncEvent"));
    }

    @Test
    void dispatch_outsideTransaction_incrementsDispatchedImmediatelyCounter() {
        final InMemoryObservance observance = new InMemoryObservance();
        final DeferredEventDispatcher obs = new DeferredEventDispatcher(delegate, asyncExecutor, observance);

        obs.dispatch(new SyncEvent());

        assertEquals(1, observance.counterValue("deferred.events.dispatchedImmediately.SyncEvent"));
    }

    @Test
    void onCommit_incrementsCommittedCounterPerEvent() throws Exception {
        final InMemoryObservance observance = new InMemoryObservance();
        final DeferredEventDispatcher obs = new DeferredEventDispatcher(delegate, asyncExecutor, observance);

        TransactionContext.runInTransaction(() -> {
            obs.dispatch(new SyncEvent());
            obs.dispatch(new SyncEvent());
            return null;
        });

        assertEquals(2, observance.counterValue("deferred.events.committed"));
    }

    @Test
    void onCommit_recordsCommitDurationTimer() throws Exception {
        final InMemoryObservance observance = new InMemoryObservance();
        final DeferredEventDispatcher obs = new DeferredEventDispatcher(delegate, asyncExecutor, observance);

        TransactionContext.runInTransaction(() -> {
            obs.dispatch(new SyncEvent());
            return null;
        });

        assertEquals(1, observance.timerCount("deferred.commit.duration"));
    }

    @Test
    void onRollback_incrementsDiscardedCounter() {
        final InMemoryObservance observance = new InMemoryObservance();
        final DeferredEventDispatcher obs = new DeferredEventDispatcher(delegate, asyncExecutor, observance);

        assertThrows(RuntimeException.class, () ->
            TransactionContext.runInTransaction(() -> {
                obs.dispatch(new SyncEvent());
                obs.dispatch(new SyncEvent());
                throw new RuntimeException("forced rollback");
            })
        );

        assertEquals(2, observance.counterValue("deferred.events.discardedOnRollback"));
        assertEquals(0, observance.counterValue("deferred.events.committed"));
    }

    @Test
    void nullObservanceRejectedByConstructor() {
        assertThrows(NullPointerException.class,
                () -> new DeferredEventDispatcher(delegate, asyncExecutor, null));
    }

    @Test
    void defaultConstructorUsesNoopObservance() throws Exception {
        // existing 2-arg constructor still works — dispatch does not throw
        final DeferredEventDispatcher defaultDispatcher = new DeferredEventDispatcher(delegate, asyncExecutor);
        TransactionContext.runInTransaction(() -> {
            defaultDispatcher.dispatch(new SyncEvent());
            return null;
        });
        assertEquals(1, delegate.dispatched.size());
    }
}
