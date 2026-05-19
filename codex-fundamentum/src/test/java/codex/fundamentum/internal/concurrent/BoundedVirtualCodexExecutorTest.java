package codex.fundamentum.internal.concurrent;

import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.concurrent.CodexExecutorConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class BoundedVirtualCodexExecutorTest {

    // --- 1: shutdown rejects new submissions ---

    @Test
    void shutdownRejectsNewSubmissions() throws InterruptedException {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(4));
        executor.shutdown();

        final AtomicBoolean ran = new AtomicBoolean(false);
        executor.submit(() -> ran.set(true));

        executor.awaitTermination(Duration.ofMillis(100));
        assertFalse(ran.get());
    }

    // --- 2: shutdownNow rejects new submissions ---

    @Test
    void shutdownNowRejectsNewSubmissions() throws InterruptedException {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(4));
        executor.shutdownNow();

        final AtomicBoolean ran = new AtomicBoolean(false);
        executor.submit(() -> ran.set(true));

        executor.awaitTermination(Duration.ofMillis(100));
        assertFalse(ran.get());
    }

    // --- 3: awaitTermination returns true after graceful completion ---

    @Test
    void awaitTerminationReturnsTrueAfterGracefulCompletion() throws InterruptedException {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(4));
        final CountDownLatch done = new CountDownLatch(1);

        executor.submit(done::countDown);
        assertTrue(done.await(2, TimeUnit.SECONDS), "task did not complete in time");

        executor.shutdown();
        assertTrue(executor.awaitTermination(Duration.ofSeconds(2)));
    }

    // --- 4: awaitTermination returns false when timeout elapses before task finishes ---

    @Test
    void awaitTerminationReturnsFalseWhenTaskOutlastsTimeout() throws InterruptedException {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(1));
        final CountDownLatch hold = new CountDownLatch(1);
        final CountDownLatch running = new CountDownLatch(1);

        executor.submit(() -> {
            running.countDown();
            try {
                hold.await();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(running.await(2, TimeUnit.SECONDS));
        executor.shutdown();
        assertFalse(executor.awaitTermination(Duration.ofMillis(50)));

        hold.countDown();
        executor.awaitTermination(Duration.ofSeconds(2));
    }

    // --- 5: shutdownNow interrupts virtual threads waiting on semaphore ---

    @Test
    void shutdownNowInterruptsTasksWaitingOnSemaphore() throws InterruptedException {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(1));
        final CountDownLatch firstRunning = new CountDownLatch(1);
        final CountDownLatch hold = new CountDownLatch(1);

        // Fills the only permit
        executor.submit(() -> {
            firstRunning.countDown();
            try {
                hold.await();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(firstRunning.await(2, TimeUnit.SECONDS), "first task did not start");

        // This virtual thread will park on semaphore.acquire()
        executor.submit(() -> {});

        executor.shutdownNow();
        assertTrue(executor.awaitTermination(Duration.ofSeconds(2)),
                "executor did not terminate after shutdownNow");
    }

    // --- 6: running tasks release permits when finished ---

    @Test
    void runningTasksReleasePermitsWhenFinished() throws InterruptedException {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(1));
        final CountDownLatch first = new CountDownLatch(1);
        final CountDownLatch second = new CountDownLatch(1);

        executor.submit(first::countDown);
        assertTrue(first.await(2, TimeUnit.SECONDS), "first task did not complete");

        // Permit must have been released; second task can acquire and run
        executor.submit(second::countDown);
        assertTrue(second.await(2, TimeUnit.SECONDS), "second task did not complete — permit may have leaked");

        executor.shutdown();
        executor.awaitTermination(Duration.ofSeconds(2));
    }

    // --- 7: interrupted waiting tasks do not leak permits ---

    @Test
    void interruptedWaitingTaskDoesNotLeakPermit() throws InterruptedException {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(1));
        final CountDownLatch firstRunning = new CountDownLatch(1);
        final CountDownLatch hold = new CountDownLatch(1);

        executor.submit(() -> {
            firstRunning.countDown();
            try {
                hold.await();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(firstRunning.await(2, TimeUnit.SECONDS));

        // Waiter will be interrupted by shutdownNow — must not leak permit
        executor.submit(() -> {});

        executor.shutdownNow();
        assertTrue(executor.awaitTermination(Duration.ofSeconds(2)),
                "executor stalled — possible permit leak from interrupted waiter");
    }

    // --- 8: null timeout is rejected ---

    @Test
    void nullTimeoutIsRejectedByAwaitTermination() {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(4));
        executor.shutdown();
        assertThrows(NullPointerException.class, () -> executor.awaitTermination(null));
    }

    // --- 9: submit is non-blocking under saturation ---

    @Test
    void submitIsNonBlockingUnderSaturation() throws InterruptedException {
        final CodexExecutor executor = CodexExecutor.of(CodexExecutorConfig.of(1));
        final CountDownLatch hold = new CountDownLatch(1);
        final CountDownLatch firstRunning = new CountDownLatch(1);

        executor.submit(() -> {
            firstRunning.countDown();
            try {
                hold.await();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(firstRunning.await(2, TimeUnit.SECONDS));

        // Permit is taken. submit() must return immediately — virtual thread parks inside.
        final long start = System.nanoTime();
        executor.submit(() -> {});
        final long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertTrue(elapsedMs < 500,
                "submit() blocked for " + elapsedMs + "ms — semaphore.acquire() must run inside the virtual thread");

        hold.countDown();
        executor.shutdown();
        executor.awaitTermination(Duration.ofSeconds(2));
    }
}
