package codex.index.internal;

import codex.fundamentum.api.observance.Observance;
import codex.index.api.IndexDocument;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexWriter;

import java.util.Objects;

import static codex.index.internal.IndexMetricNames.*;

/**
 * An {@link IndexWriter} decorator that measures upsert and delete operations via {@link Observance}.
 *
 * <p>Metrics emitted per operation:</p>
 * <ul>
 *   <li>{@code index.upsert.calls} / {@code index.delete.calls} — incremented before delegation</li>
 *   <li>{@code index.upsert.duration} / {@code index.delete.duration} — timed per call, success and failure</li>
 *   <li>{@code index.upsert.failed} / {@code index.delete.failed} — incremented when delegate throws</li>
 * </ul>
 *
 * <p>Exceptions thrown by the delegate are never swallowed — Observance does not change indexing semantics.</p>
 */
public final class ObservingIndexWriter implements IndexWriter {

    private final IndexWriter delegate;
    private final Observance observance;

    /**
     * @param delegate   the real {@link IndexWriter}; must not be null
     * @param observance observance for metrics collection; must not be null
     */
    public ObservingIndexWriter(final IndexWriter delegate, final Observance observance) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.observance = Objects.requireNonNull(observance, "observance must not be null");
    }

    @Override
    public void upsert(final IndexDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        observance.counter(UPSERT_CALLS).increment();
        try {
            observance.timer(UPSERT_DURATION).record(() -> delegate.upsert(document));
        } catch (final RuntimeException ex) {
            observance.counter(UPSERT_FAILED).increment();
            throw ex;
        }
    }

    @Override
    public void delete(final IndexDocumentId id) {
        Objects.requireNonNull(id, "id must not be null");
        observance.counter(DELETE_CALLS).increment();
        try {
            observance.timer(DELETE_DURATION).record(() -> delegate.delete(id));
        } catch (final RuntimeException ex) {
            observance.counter(DELETE_FAILED).increment();
            throw ex;
        }
    }
}
