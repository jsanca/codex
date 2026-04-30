package codex.fundamentum.api.tx;

/**
 * Callback that allows inner layers to react to the outcome of an enclosing transaction.
 * Implementations are registered via {@link TransactionContext#registerCallback(TransactionCallback)}.
 *
 * @author jsanca
 */
public interface TransactionCallback {

    /**
     * Invoked after the enclosing transaction commits successfully.
     */
    void onCommit();

    /**
     * Invoked after the enclosing transaction rolls back.
     */
    void onRollback();
}
