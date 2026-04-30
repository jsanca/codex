package codex.fundamentum.api.concurrent;

import java.util.Objects;

/**
 * Configuration bean for a {@link CodexExecutor}.
 *
 * @param maxConcurrent maximum number of tasks allowed to run concurrently;
 *                      use {@link Integer#MAX_VALUE} for effectively unbounded concurrency
 * @author jsanca
 */
public record CodexExecutorConfig(int maxConcurrent) {

    public CodexExecutorConfig {
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("maxConcurrent must be greater than zero");
        }
    }

    /**
     * Returns a config with no concurrency limit.
     *
     * @return unbounded config
     */
    public static CodexExecutorConfig unbounded() {
        return new CodexExecutorConfig(Integer.MAX_VALUE);
    }

    /**
     * Returns a config bounded to {@code maxConcurrent} concurrent tasks.
     *
     * @param maxConcurrent maximum concurrent tasks; must be greater than zero
     * @return bounded config
     */
    public static CodexExecutorConfig of(final int maxConcurrent) {
        return new CodexExecutorConfig(maxConcurrent);
    }
}
