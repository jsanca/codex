package codex.fundamentum.internal.concurrent;

import codex.fundamentum.api.concurrent.CodexExecutor;
import codex.fundamentum.api.concurrent.CodexExecutorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Virtual-thread executor bounded by a {@link Semaphore}.
 *
 * <p>The semaphore is acquired <em>inside</em> the virtual thread, so {@link #submit(Runnable)}
 * always returns immediately. If no permit is available the virtual thread parks until one is
 * released. The underlying {@link ExecutorService} provides proper lifecycle management.</p>
 *
 * @author jsanca &amp; clio
 */
public final class BoundedVirtualCodexExecutor implements CodexExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoundedVirtualCodexExecutor.class);

    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final ExecutorService executor;
    private final Semaphore semaphore;

    public BoundedVirtualCodexExecutor(final CodexExecutorConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.semaphore = new Semaphore(config.maxConcurrent());
    }

    @Override
    public void submit(final Runnable task) {
        Objects.requireNonNull(task, "task must not be null");

        if (closing.get()) {
            LOGGER.debug("Executor is closing, task rejected");
            return;
        }

        LOGGER.debug("Submitting task to executor");
        executor.submit(() -> {
            boolean acquired = false;
            try {
                LOGGER.debug("Attempting to acquire permit");
                semaphore.acquire();
                acquired = true;
                LOGGER.debug("Permit acquired, executing task");
                task.run();
            } catch (final RejectedExecutionException ex) {
                LOGGER.debug("Task rejected, executor is shutting down");
            } catch (final InterruptedException ex) {
                LOGGER.warn("Task interrupted while waiting for permit", ex);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task execution interrupted", ex);
            } finally {
                if (acquired) {
                    LOGGER.debug("Releasing permit");
                    semaphore.release();
                }
            }
        });
    }

    @Override
    public void shutdown() {
        LOGGER.info("Shutting down executor");
        closing.set(true);
        executor.shutdown();
    }
}
