package codex.fundamentum.api.tx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Carries transactional state for the current execution scope using {@link ScopedValue}.
 *
 * <p>A context is established by calling {@link #runInTransaction(ScopedValue.CallableOp)}. Within that
 * scope, any code can check {@link #isActive()}, register {@link TransactionCallback}s, or
 * attach arbitrary resources via {@link #computeIfAbsent(Class, Supplier)}.</p>
 *
 * <p>On normal completion the context notifies all registered callbacks via
 * {@link TransactionCallback#onCommit()}; on any exception it calls
 * {@link TransactionCallback#onRollback()} and re-throws.</p>
 *
 * @author jsanca
 */
public final class TransactionContext {

    private static final ScopedValue<TransactionContext> CURRENT = ScopedValue.newInstance();

    private final List<TransactionCallback> callbacks = new ArrayList<>();
    private final Map<Class<?>, Object> attachments = new HashMap<>();

    private TransactionContext() {
    }

    /**
     * Returns {@code true} if a transaction context is active on the current execution scope.
     *
     * @return {@code true} when inside a {@link #runInTransaction} scope
     */
    public static boolean isActive() {
        return CURRENT.isBound();
    }

    /**
     * Returns the active transaction context.
     *
     * @return the current context; never null
     * @throws IllegalStateException if no transaction context is active
     */
    public static TransactionContext current() {
        if (!CURRENT.isBound()) {
            throw new IllegalStateException("No active transaction context");
        }
        return CURRENT.get();
    }

    /**
     * Runs {@code action} within a fresh transaction context, then notifies all registered
     * callbacks based on the outcome.
     *
     * @param <T>    the return type of the action
     * @param action the operation to execute; must not be null
     * @return the value returned by {@code action}
     * @throws Exception any exception thrown by {@code action} (callbacks are notified first)
     */
    public static <T> T runInTransaction(final ScopedValue.CallableOp<T, Exception> action) throws Exception {
        Objects.requireNonNull(action, "action must not be null");

        final TransactionContext ctx = new TransactionContext();
        try {
            final T result = ScopedValue.where(CURRENT, ctx).call(action);
            ctx.notifyCommit();
            return result;
        } catch (final Exception ex) {
            try {
                ctx.notifyRollback();
            } catch (final Exception rollbackEx) {
                ex.addSuppressed(rollbackEx);
            }
            throw ex;
        }
    }

    /**
     * Registers a callback to be notified when this transaction context commits or rolls back.
     *
     * @param callback the callback to register; must not be null
     */
    public void registerCallback(final TransactionCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        callbacks.add(callback);
    }

    /**
     * Returns the attachment associated with {@code key}, creating and storing it via
     * {@code factory} if absent. Useful for inner layers that need per-transaction resources
     * without coupling to the event or service layer.
     *
     * @param <T>     the attachment type
     * @param key     the type token used as the lookup key; must not be null
     * @param factory supplier invoked at most once per transaction per key; must not be null
     * @return the existing or newly created attachment; never null
     */
    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(final Class<T> key, final Supplier<T> factory) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(factory, "factory must not be null");
        return (T) attachments.computeIfAbsent(key, k -> factory.get());
    }

    private void notifyCommit() {
        callbacks.forEach(TransactionCallback::onCommit);
    }

    private void notifyRollback() {
        callbacks.forEach(TransactionCallback::onRollback);
    }
}
