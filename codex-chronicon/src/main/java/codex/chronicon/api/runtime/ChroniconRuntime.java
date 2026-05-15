package codex.chronicon.api.runtime;

import codex.chronicon.api.ChroniconRepository;
import codex.chronicon.internal.ContentItemArchivedChroniconSubscriber;
import codex.chronicon.internal.ContentItemCreatedChroniconSubscriber;
import codex.chronicon.internal.ContentItemPublishedChroniconSubscriber;
import codex.chronicon.internal.ContentItemRestoredChroniconSubscriber;
import codex.chronicon.internal.ContentItemUnpublishedChroniconSubscriber;
import codex.chronicon.internal.ContentItemUpdatedChroniconSubscriber;
import codex.chronicon.internal.ContentTypeCreatedChroniconSubscriber;
import codex.chronicon.internal.MemoryChroniconRepository;
import codex.chronicon.internal.SiteCreatedChroniconSubscriber;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.runtime.CodexModuleRuntime;

import java.util.List;
import java.util.Objects;

/**
 * Module runtime for {@code codex-chronicon}.
 *
 * <p>Assembles the Chronicon audit repository and all current event subscribers.
 * It is the composition root for the Chronicon module: callers obtain a ready-to-use
 * runtime through the static factory methods and register its {@link #subscribers()}
 * with an event dispatcher.</p>
 *
 * <p>Usage (in-memory, suitable for tests and early development):</p>
 * <pre>{@code
 * ChroniconRuntime runtime = ChroniconRuntime.inMemory();
 * dispatcher.registerAll(runtime.subscribers());
 * }</pre>
 *
 * <p>Usage with a custom repository (e.g.
 * {@link codex.chronicon.internal.RecordingChroniconRepository} in tests):</p>
 * <pre>{@code
 * ChroniconRuntime runtime = ChroniconRuntime.withRepository(recordingRepository);
 * }</pre>
 *
 * <p>No ServiceLoader, Spring, global registry, or dynamic subscriber discovery is used.</p>
 */
public final class ChroniconRuntime implements CodexModuleRuntime {

    private static final String MODULE_NAME = "codex-chronicon";

    private final ChroniconRepository repository;
    private final List<CodexEventSubscriber<? extends CodexEvent>> subscribers;

    private ChroniconRuntime(final ChroniconRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.subscribers = buildSubscribers(repository);
    }

    // --- factories ---

    /**
     * Creates a {@code ChroniconRuntime} backed by an in-memory repository.
     *
     * <p>Suitable for tests and early development. Records are lost on restart.</p>
     *
     * @return a new, fully assembled {@code ChroniconRuntime}
     */
    public static ChroniconRuntime inMemory() {
        return new ChroniconRuntime(new MemoryChroniconRepository());
    }

    /**
     * Creates a {@code ChroniconRuntime} backed by the provided repository.
     *
     * <p>Useful when the caller controls the repository (e.g. a
     * {@link codex.chronicon.internal.RecordingChroniconRepository} in integration tests,
     * or a future durable implementation).</p>
     *
     * @param repository the repository to use; must not be null
     * @return a new, fully assembled {@code ChroniconRuntime}
     */
    public static ChroniconRuntime withRepository(final ChroniconRepository repository) {
        return new ChroniconRuntime(repository);
    }

    // --- CodexModuleRuntime ---

    /**
     * {@inheritDoc}
     *
     * @return {@code "codex-chronicon"}
     */
    @Override
    public String moduleName() {
        return MODULE_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return immutable list containing all current Chronicon event subscribers
     */
    @Override
    public List<CodexEventSubscriber<? extends CodexEvent>> subscribers() {
        return subscribers;
    }

    /**
     * {@inheritDoc}
     *
     * <p>No-op for now. Override when owned resources (e.g. background workers,
     * durable connections) need explicit shutdown.</p>
     */
    @Override
    public void close() {
        // no-op: this runtime owns no closeable resources
    }

    // --- accessors ---

    /**
     * Returns the configured {@link ChroniconRepository}.
     *
     * @return the audit repository; never null
     */
    public ChroniconRepository repository() {
        return repository;
    }

    // --- private helpers ---

    private static List<CodexEventSubscriber<? extends CodexEvent>> buildSubscribers(
            final ChroniconRepository repository) {

        return List.of(
                new SiteCreatedChroniconSubscriber(repository),
                new ContentTypeCreatedChroniconSubscriber(repository),
                new ContentItemCreatedChroniconSubscriber(repository),
                new ContentItemUpdatedChroniconSubscriber(repository),
                new ContentItemPublishedChroniconSubscriber(repository),
                new ContentItemUnpublishedChroniconSubscriber(repository),
                new ContentItemArchivedChroniconSubscriber(repository),
                new ContentItemRestoredChroniconSubscriber(repository)
        );
    }
}
