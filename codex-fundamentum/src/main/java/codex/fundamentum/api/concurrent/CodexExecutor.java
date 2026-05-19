package codex.fundamentum.api.concurrent;

import codex.fundamentum.internal.concurrent.BoundedVirtualCodexExecutor;

import java.time.Duration;

/**
 * Submits tasks for asynchronous execution on virtual threads with bounded concurrency.
 *
 * <p>Concurrency is controlled by a {@link java.util.concurrent.Semaphore} acquired
 * <em>inside</em> each virtual thread, so the caller of {@link #submit(Runnable)} returns
 * immediately and never blocks. If the concurrency limit is reached the virtual thread
 * parks on the semaphore until a slot is released — an inexpensive operation for virtual
 * threads.</p>
 *
 * <p>Obtain an instance via {@link #of(CodexExecutorConfig)}.
 * Call {@link #shutdown()} or {@link #shutdownNow()} when the executor is no longer needed.</p>
 *
 * @author jsanca &amp; clio
 */
public interface CodexExecutor {

    /**
     * Submits {@code task} for asynchronous execution on a virtual thread.
     * Returns immediately; the task runs when a concurrency permit is available.
     *
     * @param task the task to execute; must not be null
     */
    void submit(Runnable task);

    /**
     * Initiates a graceful shutdown: no new tasks are accepted, and tasks already
     * submitted (including those parked on a concurrency permit) are allowed to complete.
     */
    void shutdown();

    /**
     * Initiates an immediate shutdown: no new tasks are accepted, and virtual threads
     * waiting on a concurrency permit are interrupted. Running tasks may also be interrupted
     * depending on the underlying executor policy.
     */
    void shutdownNow();

    /**
     * Waits until all submitted tasks have completed, the timeout expires, or the calling
     * thread is interrupted.
     *
     * @param timeout maximum time to wait; must not be null
     * @return {@code true} if the executor terminated within the timeout; {@code false} if
     *         the timeout elapsed before termination
     */
    boolean awaitTermination(Duration timeout);

    /**
     * Creates a {@link CodexExecutor} backed by virtual threads, bounded by the given config.
     *
     * @param config executor configuration; must not be null
     * @return a new executor instance
     */
    static CodexExecutor of(final CodexExecutorConfig config) {
        return new BoundedVirtualCodexExecutor(config);
    }
}
