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
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.FieldType;
import codex.codex.api.runtime.CodexRuntime;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.index.api.IndexDocument;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexResourceType;
import codex.index.api.IndexWriter;
import codex.index.api.runtime.IndexRuntime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ConciliumRuntimeEndToEndTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("author-1"), "Author");
    private static final SiteKey SITE_KEY = SiteKey.of("test-site");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("first-post");
    private static final FieldKey TITLE_KEY = FieldKey.of("title");

    @Test
    @DisplayName("publishing content item projects to index and chronicon")
    void publishingContentItemProjectsToIndexAndChronicon() {
        final AtomicReference<CodexEventDispatcher> placeholder = new AtomicReference<>(event -> {});
        final CodexRuntime core = CodexRuntime.inMemory(event -> placeholder.get().dispatch(event));

        final LocalRecordingIndexWriter indexWriter = new LocalRecordingIndexWriter();
        final IndexRuntime index = IndexRuntime.withWriter(
                core.contentItemProjectionReader(), indexWriter);

        final LocalRecordingChroniconRepository chroniconRepository = new LocalRecordingChroniconRepository();
        final ChroniconRuntime chronicon = ChroniconRuntime.withRepository(chroniconRepository);

        final List<CodexEventSubscriber<? extends CodexEvent>> allSubs = new ArrayList<>();
        allSubs.addAll(index.subscribers());
        allSubs.addAll(chronicon.subscribers());
        placeholder.set(new LocalCodexEventDispatcher(allSubs));

        final ConciliumRuntime concilium = ConciliumRuntime.compose(core, index, chronicon);

        // 1. Create site
        concilium.coreRuntime().siteService().create(
                CreateSiteCommand.of(SITE_KEY, "Test Site"), ACTOR);

        // 2. Create content type
        concilium.coreRuntime().contentTypeService().create(
                CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);

        // 3. Add title field
        concilium.coreRuntime().contentTypeService().addField(
                AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                        Field.builder()
                                .key(TITLE_KEY)
                                .displayName("Title")
                                .type(FieldType.TEXT)
                                .required(true)
                                .build()),
                ACTOR);

        // 4. Activate content type
        concilium.coreRuntime().contentTypeService().activate(
                ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);

        // 5. Create content item
        final ContentItem item = concilium.coreRuntime().contentItemService().create(
                CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                        Map.of(TITLE_KEY, (Object) "My First Post")),
                ACTOR);

        // 6. Publish content item
        final ContentItem publishedItem = concilium.coreRuntime().contentItemService().publish(
                PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);

        // --- Index Assertions ---
        final List<IndexDocument> upserts = indexWriter.upserts();
        assertEquals(1, upserts.size(), "IndexWriter should receive exactly one upsert");

        final IndexDocument doc = upserts.get(0);
        assertEquals(IndexResourceType.CONTENT_ITEM, doc.resourceType(), "Resource type must be CONTENT_ITEM");

        final String docId = doc.id().value();
        assertTrue(docId.contains(SITE_KEY.value()), "Doc ID must contain site key");
        assertTrue(docId.contains(CT_KEY.value()), "Doc ID must contain content type key");
        assertTrue(docId.contains(ITEM_KEY.value()), "Doc ID must contain content item key");

        assertEquals("My First Post", doc.title(), "Title must match the published title");

        final Map<String, String> metadata = doc.metadata();
        assertNotNull(metadata.get("contentItemId"), "Metadata must contain contentItemId");
        assertEquals(ITEM_KEY.value(), metadata.get("contentItemKey"), "Metadata must contain contentItemKey");
        assertEquals(CT_KEY.value(), metadata.get("contentTypeKey"), "Metadata must contain contentTypeKey");
        assertNotNull(metadata.get("contentTypeVersionId"), "Metadata must contain contentTypeVersionId");
        assertNotNull(metadata.get("contentRevisionId"), "Metadata must contain contentRevisionId");

        // --- Chronicon Assertions ---
        final List<AuditRecord> records = chroniconRepository.findAll();
        assertFalse(records.isEmpty(), "ChroniconRepository should have at least one record");

        final Optional<AuditRecord> publishRecord = records.stream()
                .filter(r -> r.action() == AuditAction.PUBLISHED && "content-item".equals(r.subject().type()))
                .findFirst();

        assertTrue(publishRecord.isPresent(), "Must have an audit record for PUBLISHED content-item");
        final AuditRecord audit = publishRecord.get();
        assertEquals(ITEM_KEY.value(), audit.subject().key(), "Subject key must match content item key");
        assertEquals(ACTOR.id(), audit.actorId(), "Actor ID must match the publisher");
        
        // --- Core Assertions ---
        assertFalse(core.recordedEvents().isEmpty(), "Core event recorder should capture events");
    }

    private static final class LocalRecordingIndexWriter implements IndexWriter {
        private final List<IndexDocument> upserted = new ArrayList<>();

        @Override
        public void upsert(final IndexDocument document) {
            Objects.requireNonNull(document);
            upserted.add(document);
        }

        @Override
        public void delete(final IndexDocumentId id) {
        }

        List<IndexDocument> upserts() {
            return List.copyOf(upserted);
        }
    }

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
            return saved.stream().filter(r -> r.id().equals(id)).findFirst();
        }

        @Override
        public List<AuditRecord> findBySubject(final AuditSubject subject) {
            return saved.stream().filter(r -> r.subject().equals(subject)).toList();
        }

        @Override
        public List<AuditRecord> findByActor(final ActorId actorId) {
            return saved.stream().filter(r -> r.actorId().equals(actorId)).toList();
        }

        @Override
        public List<AuditRecord> findByAction(final AuditAction action) {
            return saved.stream().filter(r -> r.action().equals(action)).toList();
        }

        @Override
        public List<AuditRecord> findAll() {
            return List.copyOf(saved);
        }
    }
}
