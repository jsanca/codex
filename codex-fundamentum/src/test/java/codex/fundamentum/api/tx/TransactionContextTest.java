package codex.fundamentum.api.tx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TransactionContextTest {

    @Test
    void isActive_returnsFalse_whenOutsideTransaction() {
        assertFalse(TransactionContext.isActive());
    }

    @Test
    void isActive_returnsTrue_insideRunInTransaction() throws Exception {
        TransactionContext.runInTransaction(() -> {
            assertTrue(TransactionContext.isActive());
            return null;
        });
    }

    @Test
    void registeredCallback_onCommit_calledAfterSuccessfulTransaction() throws Exception {
        RecordingCallback callback = new RecordingCallback();

        TransactionContext.runInTransaction(() -> {
            TransactionContext.current().registerCallback(callback);
            return null;
        });

        assertTrue(callback.committed);
        assertFalse(callback.rolledBack);
    }

    @Test
    void registeredCallback_onRollback_calledWhenTransactionThrows() {
        RecordingCallback callback = new RecordingCallback();

        assertThrows(RuntimeException.class, () ->
            TransactionContext.runInTransaction(() -> {
                TransactionContext.current().registerCallback(callback);
                throw new RuntimeException("forced rollback");
            })
        );

        assertFalse(callback.committed);
        assertTrue(callback.rolledBack);
    }

    @Test
    void nestedTransactions_doNotShareContext() throws Exception {
        List<TransactionContext> captured = new ArrayList<>();

        TransactionContext.runInTransaction(() -> {
            captured.add(TransactionContext.current());

            TransactionContext.runInTransaction(() -> {
                captured.add(TransactionContext.current());
                return null;
            });

            return null;
        });

        assertEquals(2, captured.size());
        assertNotSame(captured.get(0), captured.get(1));
    }

    @Test
    void parallelTransactions_doNotShareContext() throws Exception {
        AtomicReference<TransactionContext> firstCtx = new AtomicReference<>();
        AtomicReference<TransactionContext> secondCtx = new AtomicReference<>();

        Thread first = Thread.ofVirtual().start(() -> {
            try {
                TransactionContext.runInTransaction(() -> {
                    firstCtx.set(TransactionContext.current());
                    return null;
                });
            } catch (Exception ignored) {}
        });

        Thread second = Thread.ofVirtual().start(() -> {
            try {
                TransactionContext.runInTransaction(() -> {
                    secondCtx.set(TransactionContext.current());
                    return null;
                });
            } catch (Exception ignored) {}
        });

        first.join();
        second.join();

        assertNotNull(firstCtx.get());
        assertNotNull(secondCtx.get());
        assertNotSame(firstCtx.get(), secondCtx.get());
    }

    private static final class RecordingCallback implements TransactionCallback {

        boolean committed = false;
        boolean rolledBack = false;

        @Override
        public void onCommit() {
            committed = true;
        }

        @Override
        public void onRollback() {
            rolledBack = true;
        }
    }
}
