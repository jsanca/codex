package codex.concilium.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditRecordId;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.chronicon.api.runtime.ChroniconRuntime;
import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.FieldType;
import codex.codex.api.projection.ContentItemProjectionReader;
import codex.codex.api.runtime.CodexRuntime;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.index.api.IndexDocument;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexWriter;
import codex.index.api.runtime.IndexRuntime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ConciliumRuntime}.
 *
 * <p>Verifies assembly, subscriber composition, and the full event pipeline from
 * core domain operations through to module subscribers — without Spring, ServiceLoader,
 * or external infrastructure.</p>
 */
class ConciliumRuntimeTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("article");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("first-post");

    // --- factory: inMemory ---

    @Test
    void inMemoryCreatesRuntime() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        assertNotNull(runtime);
    }

    @Test
    void moduleNameReturnsConcilium() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        assertEquals("codex-concilium", runtime.moduleName());
    }

    @Test
    void coreRuntimeIsNotNull() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        assertNotNull(runtime.coreRuntime());
    }

    @Test
    void indexRuntimeIsNotNull() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        assertNotNull(runtime.indexRuntime());
    }

    @Test
    void chroniconRuntimeIsNotNull() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        assertNotNull(runtime.chroniconRuntime());
    }

    @Test
    void eventDispatcherIsNotNull() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        assertNotNull(runtime.eventDispatcher());
    }

    @Test
    void subscribersContainsIndexAndChroniconSubscribers() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        // 1 from IndexRuntime + 3 from ChroniconRuntime
        assertEquals(4, runtime.subscribers().size());
    }

    @Test
    void subscribersSnapshotIsImmutable() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        final List<CodexEventSubscriber<? extends CodexEvent>> subscribers = runtime.subscribers();
        assertThrows(UnsupportedOperationException.class, () -> subscribers.add(null));
    }

    // --- factory: compose ---

    @Test
    void composeRejectsNullCoreRuntime() {
        final IndexRuntime index = IndexRuntime.inMemory(emptyProjectionReader());
        final ChroniconRuntime chronicon = ChroniconRuntime.inMemory();
        assertThrows(NullPointerException.class,
                () -> ConciliumRuntime.compose(null, index, chronicon));
    }

    @Test
    void composeRejectsNullIndexRuntime() {
        final CodexRuntime core = CodexRuntime.inMemory();
        final ChroniconRuntime chronicon = ChroniconRuntime.inMemory();
        assertThrows(NullPointerException.class,
                () -> ConciliumRuntime.compose(core, null, chronicon));
    }

    @Test
    void composeRejectsNullChroniconRuntime() {
        final CodexRuntime core = CodexRuntime.inMemory();
        final IndexRuntime index = IndexRuntime.inMemory(emptyProjectionReader());
        assertThrows(NullPointerException.class,
                () -> ConciliumRuntime.compose(core, index, null));
    }

    @Test
    void composeUsesProvidedRuntimes() {
        final CodexRuntime core = CodexRuntime.inMemory();
        final IndexRuntime index = IndexRuntime.inMemory(core.contentItemProjectionReader());
        final ChroniconRuntime chronicon = ChroniconRuntime.inMemory();

        final ConciliumRuntime concilium = ConciliumRuntime.compose(core, index, chronicon);

        assertSame(core, concilium.coreRuntime());
        assertSame(index, concilium.indexRuntime());
        assertSame(chronicon, concilium.chroniconRuntime());
    }

    // --- close ---

    @Test
    void closeDoesNotThrow() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        assertDoesNotThrow(runtime::close);
    }

    @Test
    void closeIsIdempotent() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
        assertDoesNotThrow(runtime::close);
        assertDoesNotThrow(runtime::close);
    }

    // --- integration: full event pipeline ---

    /**
     * Proves that a domain operation executed through core services emits an event that
     * is routed through the Concilium dispatcher to both Index and Chronicon subscribers.
     *
     * <p>Uses a forwarding dispatcher to break the circular construction dependency, then
     * runs a full publish flow. After publication:</p>
     * <ul>
     *   <li>{@code LocalRecordingChroniconRepository} should contain three audit records
     *       (site created, content type created, content item published).</li>
     *   <li>{@code LocalRecordingIndexWriter} should contain one upserted index document.</li>
     * </ul>
     */
    @Test
    void fullPublishFlowReachesBothIndexAndChronicon() {
        // A forwarding dispatcher breaks the circular dependency:
        // CodexRuntime needs the module dispatcher at construction time, but IndexRuntime
        // needs core.contentItemProjectionReader(), which requires CodexRuntime to exist first.
        final AtomicReference<CodexEventDispatcher> placeholder =
                new AtomicReference<>(event -> {});
        final CodexRuntime core = CodexRuntime.inMemory(
                event -> placeholder.get().dispatch(event));

        final LocalRecordingIndexWriter recordingWriter = new LocalRecordingIndexWriter();
        final IndexRuntime index = IndexRuntime.withWriter(
                core.contentItemProjectionReader(), recordingWriter);

        final LocalRecordingChroniconRepository recordingRepo = new LocalRecordingChroniconRepository();
        final ChroniconRuntime chronicon = ChroniconRuntime.withRepository(recordingRepo);

        // Wire the combined module dispatcher into the core's event pipeline.
        final List<CodexEventSubscriber<? extends CodexEvent>> allSubs = new ArrayList<>();
        allSubs.addAll(index.subscribers());
        allSubs.addAll(chronicon.subscribers());
        placeholder.set(new LocalCodexEventDispatcher(allSubs));

        final ConciliumRuntime concilium = ConciliumRuntime.compose(core, index, chronicon);

        // Full authoring flow: site → content type → fields → activate → item → publish
        concilium.coreRuntime().siteService().create(
                CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);

        concilium.coreRuntime().contentTypeService().create(
                CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR);

        concilium.coreRuntime().contentTypeService().addField(
                AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                        Field.builder()
                                .key(FieldKey.TITLE)
                                .displayName("Title")
                                .type(FieldType.TEXT)
                                .required(true)
                                .build()),
                ACTOR);

        concilium.coreRuntime().contentTypeService().activate(
                ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);

        concilium.coreRuntime().contentItemService().create(
                CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                        Map.of(FieldKey.TITLE, "First Post")),
                ACTOR);

        concilium.coreRuntime().contentItemService().publish(
                PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);

        // Core recorded all events
        assertFalse(concilium.coreRuntime().recordedEvents().isEmpty(),
                "core event recorder should have captured events");

        // Chronicon received site created, content type created, content item published
        final List<AuditRecord> auditRecords = recordingRepo.findAll();
        assertEquals(3, auditRecords.size(),
                "chronicon should have one audit record per subscribed event type");

        // Index received content item published
        final List<IndexDocument> upserts = recordingWriter.upserts();
        assertEquals(1, upserts.size(),
                "index should have one upserted document for the published content item");
        assertTrue(upserts.get(0).id().value().contains("article"),
                "index document id should reference the content type");
    }

    /**
     * Verifies the simpler {@link ConciliumRuntime#inMemory()} convenience path:
     * a site creation reaches Chronicon through the pre-wired module dispatcher.
     */
    @Test
    void inMemorySiteCreationReachesChronicon() {
        final ConciliumRuntime runtime = ConciliumRuntime.inMemory();

        runtime.coreRuntime().siteService().create(
                CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);

        // MemoryChroniconRepository.findAll() returns all saved records
        final List<AuditRecord> records =
                runtime.chroniconRuntime().repository().findAll();
        assertEquals(1, records.size(),
                "SiteCreatedEvent should produce one audit record in chronicon");
        assertEquals("site", records.get(0).subject().type());
    }

    // --- private helpers ---

    private static ContentItemProjectionReader emptyProjectionReader() {
        return new EmptyContentItemProjectionReader();
    }

    // --- inner types ---

    private static final class EmptyContentItemProjectionReader implements ContentItemProjectionReader {
        @Override
        public Optional<ContentItem> findContentItem(
                final SiteKey siteKey,
                final ContentTypeKey contentTypeKey,
                final ContentItemKey key) {
            return Optional.empty();
        }

        @Override
        public Optional<ContentRevision> findContentRevision(
                final ContentRevisionId revisionId) {
            return Optional.empty();
        }
    }

    /**
     * Local recording implementation of {@link IndexWriter} for use in this test.
     * Avoids any dependency on internal recording helpers from {@code codex-index}.
     */
    private static final class LocalRecordingIndexWriter implements IndexWriter {

        private final List<IndexDocument> upserted = new ArrayList<>();

        @Override
        public void upsert(final IndexDocument document) {
            Objects.requireNonNull(document);
            upserted.add(document);
        }

        @Override
        public void delete(final IndexDocumentId id) {
            // not used in these tests
        }

        List<IndexDocument> upserts() {
            return List.copyOf(upserted);
        }
    }

    /**
     * Local recording implementation of {@link ChroniconRepository} for use in this test.
     * Avoids any dependency on internal recording helpers from {@code codex-chronicon}.
     */
    private static final class LocalRecordingChroniconRepository implements ChroniconRepository {

        private final List<AuditRecord> saved = new ArrayList<>();

        @Override
        public AuditRecord save(final AuditRecord record) {
            Objects.requireNonNull(record);
            saved.add(record);
            return record;
        }

        @Override
        public Optional<AuditRecord> findById(final AuditRecordId id) {
            Objects.requireNonNull(id);
            return saved.stream().filter(r -> r.id().equals(id)).findFirst();
        }

        @Override
        public List<AuditRecord> findBySubject(final AuditSubject subject) {
            Objects.requireNonNull(subject);
            return saved.stream().filter(r -> r.subject().equals(subject)).toList();
        }

        @Override
        public List<AuditRecord> findByActor(final ActorId actorId) {
            Objects.requireNonNull(actorId);
            return saved.stream().filter(r -> r.actorId().equals(actorId)).toList();
        }

        @Override
        public List<AuditRecord> findByAction(final AuditAction action) {
            Objects.requireNonNull(action);
            return saved.stream().filter(r -> r.action().equals(action)).toList();
        }

        @Override
        public List<AuditRecord> findAll() {
            return List.copyOf(saved);
        }
    }
}
