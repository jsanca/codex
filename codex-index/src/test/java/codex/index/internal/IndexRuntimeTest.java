package codex.index.internal;

import codex.codex.api.model.entity.ContentItem;
import codex.index.api.runtime.IndexRuntime;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemArchivedEvent;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.codex.api.projection.ContentItemProjectionReader;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.index.api.IndexWriter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IndexRuntime}.
 *
 * <p>Verifies assembly, subscriber composition, and dispatcher integration
 * without Spring, ServiceLoader, or external index infrastructure.</p>
 */
class IndexRuntimeTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.of("rev-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    // --- factory: inMemory ---

    @Test
    void inMemoryCreatesRuntime() {
        final IndexRuntime runtime = IndexRuntime.inMemory(stubReader());
        assertNotNull(runtime);
    }

    @Test
    void moduleNameReturnsCodexIndex() {
        final IndexRuntime runtime = IndexRuntime.inMemory(stubReader());
        assertEquals("codex-index", runtime.moduleName());
    }

    @Test
    void indexWriterIsNotNull() {
        final IndexRuntime runtime = IndexRuntime.inMemory(stubReader());
        assertNotNull(runtime.indexWriter());
    }

    @Test
    void inMemoryUsesNoOpIndexWriter() {
        final IndexRuntime runtime = IndexRuntime.inMemory(stubReader());
        assertInstanceOf(NoOpIndexWriter.class, runtime.indexWriter());
    }

    @Test
    void subscribersContainsFourSubscribers() {
        final IndexRuntime runtime = IndexRuntime.inMemory(stubReader());
        assertEquals(4, runtime.subscribers().size());
    }

    @Test
    void subscribersSnapshotIsImmutable() {
        final IndexRuntime runtime = IndexRuntime.inMemory(stubReader());
        final List<CodexEventSubscriber<? extends CodexEvent>> subscribers = runtime.subscribers();
        assertThrows(UnsupportedOperationException.class, () -> subscribers.add(null));
    }

    // --- factory: withWriter ---

    @Test
    void withWriterRejectsNullProjectionReader() {
        final RecordingIndexWriter writer = new RecordingIndexWriter();
        assertThrows(NullPointerException.class, () -> IndexRuntime.withWriter(null, writer));
    }

    @Test
    void withWriterRejectsNullIndexWriter() {
        assertThrows(NullPointerException.class, () -> IndexRuntime.withWriter(stubReader(), null));
    }

    @Test
    void withWriterUsesProvidedWriter() {
        final RecordingIndexWriter writer = new RecordingIndexWriter();
        final IndexRuntime runtime = IndexRuntime.withWriter(stubReader(), writer);
        assertSame(writer, runtime.indexWriter());
    }

    // --- close ---

    @Test
    void closeDoesNotThrow() {
        final IndexRuntime runtime = IndexRuntime.inMemory(stubReader());
        assertDoesNotThrow(runtime::close);
    }

    @Test
    void closeIsIdempotent() {
        final IndexRuntime runtime = IndexRuntime.inMemory(stubReader());
        assertDoesNotThrow(runtime::close);
        assertDoesNotThrow(runtime::close);
    }

    // --- dispatcher integration ---

    @Test
    void runtimeSubscriberWorksWithLocalCodexEventDispatcher() {
        final StubContentItemProjectionReader reader = new StubContentItemProjectionReader();
        final RecordingIndexWriter writer = new RecordingIndexWriter();
        final IndexRuntime runtime = IndexRuntime.withWriter(reader, writer);

        final ContentItem item = ContentItem.builder()
                .id(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .key(ITEM_KEY)
                .currentWorkingRevisionId(REVISION_ID)
                .owner(ACTOR_ID)
                .createdBy(ACTOR_ID)
                .updatedBy(ACTOR_ID)
                .updatedAt(NOW)
                .build();

        final ContentRevision revision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .values(Map.of(FieldKey.TITLE, "Hello World", FieldKey.of("summary"), "A great post"))
                .createdBy(ACTOR_ID)
                .build();

        reader.saveItem(item);
        reader.saveRevision(revision);

        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(runtime.subscribers());

        dispatcher.dispatch(new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, REVISION_ID,
                Actor.human(ACTOR_ID, "Test User"), NOW));

        assertEquals(1, writer.upserts().size(), "one index document should have been upserted");
        assertEquals("content-item:acme:blog-post:my-post", writer.upserts().get(0).id().value());
    }

    @Test
    void runtimeSubscriberHandlesArchivedEventAndCallsDelete() {
        final RecordingIndexWriter writer = new RecordingIndexWriter();
        final IndexRuntime runtime = IndexRuntime.withWriter(stubReader(), writer);

        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(runtime.subscribers());

        dispatcher.dispatch(new ContentItemArchivedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY,
                Actor.human(ACTOR_ID, "Test User"), NOW));

        assertEquals(1, writer.deletes().size(), "one document id should have been deleted");
        assertEquals("content-item:acme:blog-post:my-post", writer.deletes().get(0).value());
    }

    // --- private helpers ---

    private static ContentItemProjectionReader stubReader() {
        return new StubContentItemProjectionReader();
    }

    // --- inner types ---

    /**
     * Deterministic stub for {@link ContentItemProjectionReader}. Does not use Mockito
     * or any internal repository.
     */
    private static final class StubContentItemProjectionReader implements ContentItemProjectionReader {

        private final Map<String, ContentItem> items = new HashMap<>();
        private final Map<String, ContentRevision> revisions = new HashMap<>();

        void saveItem(final ContentItem item) {
            items.put(item.siteKey().value() + ":" + item.contentTypeKey().value() + ":" + item.key().value(), item);
        }

        void saveRevision(final ContentRevision revision) {
            revisions.put(revision.id().value(), revision);
        }

        @Override
        public Optional<ContentItem> findContentItem(
                final SiteKey siteKey, final ContentTypeKey contentTypeKey, final ContentItemKey key) {
            return Optional.ofNullable(
                    items.get(siteKey.value() + ":" + contentTypeKey.value() + ":" + key.value()));
        }

        @Override
        public Optional<ContentRevision> findContentRevision(final ContentRevisionId revisionId) {
            return Optional.ofNullable(revisions.get(revisionId.value()));
        }
    }
}
