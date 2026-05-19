package codex.index.api.runtime;

import codex.codex.api.projection.ContentItemProjectionReader;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.observance.Observance;
import codex.fundamentum.api.runtime.CodexModuleRuntime;
import codex.index.api.IndexWriter;
import codex.index.internal.ContentItemArchivedIndexingSubscriber;
import codex.index.internal.ContentItemDeletedIndexingSubscriber;
import codex.index.internal.ContentItemIndexDocumentMapper;
import codex.index.internal.ContentItemPublishedIndexingSubscriber;
import codex.index.internal.ContentItemUnpublishedIndexingSubscriber;
import codex.index.internal.NoOpIndexWriter;
import codex.index.internal.ObservingIndexWriter;
import codex.index.internal.ReaderContentItemProjectionSource;

import java.util.List;
import java.util.Objects;

/**
 * Module runtime for {@code codex-index}.
 *
 * <p>Assembles the index writer, projection source, document mapper, and indexing subscriber
 * into a single composition root. Callers obtain a ready-to-use runtime through the static
 * factory methods and register its {@link #subscribers()} with an event dispatcher.</p>
 *
 * <p>Usage with the default no-op writer (structurally wired, no infrastructure required):</p>
 * <pre>{@code
 * IndexRuntime runtime = IndexRuntime.inMemory(core.contentItemProjectionReader());
 * dispatcher.registerAll(runtime.subscribers());
 * }</pre>
 *
 * <p>Usage with a custom writer (e.g. {@link codex.index.internal.RecordingIndexWriter}
 * in integration tests):</p>
 * <pre>{@code
 * IndexRuntime runtime = IndexRuntime.withWriter(stubReader, recordingWriter);
 * }</pre>
 *
 * <p>Usage with observability (e.g. in integration tests or production assembly):</p>
 * <pre>{@code
 * IndexRuntime runtime = IndexRuntime.withWriter(reader, writer, observance);
 * }</pre>
 *
 * <p>In all cases the provided {@link IndexWriter} is wrapped by an
 * {@link codex.index.internal.ObservingIndexWriter} that records upsert/delete counts,
 * durations, and failures. {@link #indexWriter()} returns the observed wrapper.</p>
 *
 * <p>No ServiceLoader, Spring, global registry, or dynamic subscriber discovery is used.</p>
 */
public final class IndexRuntime implements CodexModuleRuntime {

    private static final String MODULE_NAME = "codex-index";

    private final IndexWriter indexWriter;
    private final List<CodexEventSubscriber<? extends CodexEvent>> subscribers;

    private IndexRuntime(final ContentItemProjectionReader projectionReader,
                         final IndexWriter indexWriter,
                         final Observance observance) {
        Objects.requireNonNull(projectionReader, "projectionReader must not be null");
        Objects.requireNonNull(observance, "observance must not be null");
        final IndexWriter observed = new ObservingIndexWriter(
                Objects.requireNonNull(indexWriter, "indexWriter must not be null"),
                observance);
        this.indexWriter = observed;
        this.subscribers = buildSubscribers(projectionReader, observed);
    }

    // --- factories ---

    /**
     * Creates an {@code IndexRuntime} backed by a {@link NoOpIndexWriter} and no-op observance.
     *
     * <p>Suitable for environments where indexing is structurally wired but no external
     * index infrastructure is available. Events are consumed and discarded silently.</p>
     *
     * @param projectionReader the public core projection contract; must not be null
     * @return a new, fully assembled {@code IndexRuntime}
     */
    public static IndexRuntime inMemory(final ContentItemProjectionReader projectionReader) {
        return inMemory(projectionReader, Observance.noop());
    }

    /**
     * Creates an {@code IndexRuntime} backed by a {@link NoOpIndexWriter} and the provided
     * {@link Observance}.
     *
     * <p>Suitable for environments where indexing is structurally wired but no external
     * index infrastructure is available. Metrics are captured when a non-noop
     * {@code Observance} is provided.</p>
     *
     * @param projectionReader the public core projection contract; must not be null
     * @param observance       observance for index operation metrics; must not be null
     * @return a new, fully assembled {@code IndexRuntime}
     */
    public static IndexRuntime inMemory(
            final ContentItemProjectionReader projectionReader,
            final Observance observance) {
        return new IndexRuntime(projectionReader, new NoOpIndexWriter(), observance);
    }

    /**
     * Creates an {@code IndexRuntime} backed by the provided {@link IndexWriter}.
     *
     * <p>Useful when the caller controls the writer (e.g. a
     * {@link codex.index.internal.RecordingIndexWriter} in integration tests, or a future
     * durable adapter such as OpenSearch or Lucene).</p>
     *
     * @param projectionReader the public core projection contract; must not be null
     * @param indexWriter      the writer to send index documents to; must not be null
     * @return a new, fully assembled {@code IndexRuntime}
     */
    public static IndexRuntime withWriter(
            final ContentItemProjectionReader projectionReader,
            final IndexWriter indexWriter) {
        return new IndexRuntime(projectionReader, indexWriter, Observance.noop());
    }

    /**
     * Creates an {@code IndexRuntime} backed by the provided {@link IndexWriter} and
     * {@link Observance}, wrapping the writer with an {@link ObservingIndexWriter}.
     *
     * @param projectionReader the public core projection contract; must not be null
     * @param indexWriter      the writer to send index documents to; must not be null
     * @param observance       observance for index operation metrics; must not be null
     * @return a new, fully assembled {@code IndexRuntime}
     */
    public static IndexRuntime withWriter(
            final ContentItemProjectionReader projectionReader,
            final IndexWriter indexWriter,
            final Observance observance) {
        return new IndexRuntime(projectionReader, indexWriter, observance);
    }

    // --- CodexModuleRuntime ---

    /**
     * {@inheritDoc}
     *
     * @return {@code "codex-index"}
     */
    @Override
    public String moduleName() {
        return MODULE_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return immutable list containing all current index event subscribers
     */
    @Override
    public List<CodexEventSubscriber<? extends CodexEvent>> subscribers() {
        return subscribers;
    }

    /**
     * {@inheritDoc}
     *
     * <p>No-op: this runtime owns no closeable resources.</p>
     */
    @Override
    public void close() {
        // no-op: this runtime owns no closeable resources
    }

    // --- accessors ---

    /**
     * Returns the configured {@link IndexWriter}.
     *
     * @return the index writer; never null
     */
    public IndexWriter indexWriter() {
        return indexWriter;
    }

    // --- private helpers ---

    private static List<CodexEventSubscriber<? extends CodexEvent>> buildSubscribers(
            final ContentItemProjectionReader projectionReader,
            final IndexWriter indexWriter) {

        final ReaderContentItemProjectionSource source =
                new ReaderContentItemProjectionSource(projectionReader);

        return List.of(
                new ContentItemPublishedIndexingSubscriber(source, indexWriter, new ContentItemIndexDocumentMapper()),
                new ContentItemUnpublishedIndexingSubscriber(indexWriter),
                new ContentItemArchivedIndexingSubscriber(indexWriter),
                new ContentItemDeletedIndexingSubscriber(indexWriter)
        );
    }
}
