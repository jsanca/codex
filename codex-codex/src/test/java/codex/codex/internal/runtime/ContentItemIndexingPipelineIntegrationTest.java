package codex.codex.internal.runtime;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexResourceType;
import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.FieldType;
import codex.codex.internal.index.RecordingIndexWriter;
import codex.codex.internal.service.TestCodexContext;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.tx.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests proving the full event-to-index pipeline:
 * <pre>
 * publish content item
 *   → ContentItemPublishedEvent
 *      → DeferredEventDispatcher
 *         → CompositeCodexEventDispatcher
 *            → EventRecorder
 *            → LocalCodexEventDispatcher
 *               → ContentItemPublishedIndexingSubscriber
 *                  → RecordingIndexWriter
 * </pre>
 */
class ContentItemIndexingPipelineIntegrationTest {

    private static final Actor ACTOR = Actor.system("integration-test");
    private static final SiteKey SITE_KEY = SiteKey.of("authoring-site");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("welcome-to-codex");

    private RecordingIndexWriter indexWriter;
    private TestCodexContext ctx;

    @BeforeEach
    void setUp() throws Exception {
        indexWriter = new RecordingIndexWriter();
        ctx = TestCodexContext.createWithIndexWriter(indexWriter);

        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            ctx.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder().key(FieldKey.TITLE).type(FieldType.TEXT).required(true).build()),
                    ACTOR);
            ctx.contentTypeService().activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            ctx.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "Welcome to Codex")),
                    ACTOR);
            return null;
        });

        ctx.clearEvents();
        indexWriter.clear();
    }

    // --- 1: publish routes event to index writer ---

    @Test
    @DisplayName("publishing content item routes event to indexing subscriber")
    void publishingContentItemRoutesEventToIndexingSubscriber() throws Exception {
        // inside transaction: no index upsert yet (event is deferred)
        TransactionContext.runInTransaction(() -> {
            ctx.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
            assertTrue(indexWriter.upserts().isEmpty(),
                    "index must not be written before transaction commits");
            return null;
        });

        // after commit: event dispatched, subscriber invoked
        ctx.assertSingleEventOfType(ContentItemPublishedEvent.class);

        assertEquals(1, indexWriter.upserts().size());
        final IndexDocument doc = indexWriter.upserts().getFirst();
        assertEquals(IndexResourceType.CONTENT_ITEM, doc.resourceType());
        assertEquals(SITE_KEY, doc.siteKey());
        assertEquals("content-item:authoring-site:blog-post:welcome-to-codex", doc.id().value());
        assertEquals("Welcome to Codex", doc.title());
    }

    // --- 2: rollback does not write to index ---

    @Test
    @DisplayName("publishing content item rollback does not index")
    void publishingContentItemRollbackDoesNotIndex() {
        assertThrows(RuntimeException.class, () ->
                TransactionContext.runInTransaction(() -> {
                    ctx.contentItemService().publish(
                            PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
                    throw new RuntimeException("forced rollback");
                }));

        ctx.assertNoEventsOfType(ContentItemPublishedEvent.class);
        assertTrue(indexWriter.upserts().isEmpty(),
                "index must not be written after rollback");
    }

    // --- 3: default runtime (NoOpIndexWriter) does not fail ---

    @Test
    @DisplayName("runtime with default no-op index writer can publish content item")
    void runtimeWithDefaultNoOpIndexWriterCanPublishContentItem() throws Exception {
        // Use standard inMemory() without a custom writer — should not throw
        final TestCodexContext defaultCtx = TestCodexContext.create();

        TransactionContext.runInTransaction(() -> {
            defaultCtx.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            defaultCtx.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder().key(FieldKey.TITLE).type(FieldType.TEXT).required(true).build()),
                    ACTOR);
            defaultCtx.contentTypeService().activate(
                    ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            defaultCtx.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "Hello")),
                    ACTOR);
            return null;
        });

        assertDoesNotThrow(() -> TransactionContext.runInTransaction(() ->
                defaultCtx.contentItemService().publish(
                        PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR)));
    }
}
