package codex.codex.internal.index;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexResourceType;
import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.FieldType;
import codex.codex.internal.repository.MemoryContentItemRepository;
import codex.codex.internal.repository.MemoryContentRevisionRepository;
import codex.codex.internal.service.TestCodexContext;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.tx.TransactionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test proving that a {@link ContentItemPublishedEvent} produced by the full
 * Codex service pipeline can be consumed by {@link ContentItemPublishedIndexingSubscriber}
 * to produce an {@link IndexDocument} in a {@link RecordingIndexWriter}.
 * <p>
 * Flow:
 * <pre>
 *   publish content item
 *     → ContentItemPublishedEvent
 *        → ContentItemPublishedIndexingSubscriber
 *           → IndexWriter.upsert(IndexDocument)
 * </pre>
 * This test does not require OpenSearch, myIR, Lucene, or any external system.
 */
class ContentItemIndexingIntegrationTest {

    private static final Actor ACTOR = Actor.system("integration-test");
    private static final SiteKey SITE_KEY = SiteKey.of("authoring-site");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("welcome-to-codex");

    @Test
    @DisplayName("publish content item event produces an index document in RecordingIndexWriter")
    void publishEventProducesIndexDocument() throws Exception {
        // --- 1: wire the Codex service pipeline ---
        final TestCodexContext ctx = TestCodexContext.create();

        // --- 2: create content type with a title field ---
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            ctx.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder().key(FieldKey.TITLE).type(FieldType.TEXT).required(true).build()),
                    ACTOR);
            ctx.contentTypeService().activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });

        // --- 3: create and publish a content item ---
        final ContentItem publishedItem = TransactionContext.runInTransaction(() -> {
            ctx.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "Welcome to Codex")),
                    ACTOR);
            return ctx.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
        });

        // --- 4: extract the ContentItemPublishedEvent ---
        final ContentItemPublishedEvent event = ctx.assertSingleEventOfType(ContentItemPublishedEvent.class);

        // --- 5: wire the subscriber with its own in-memory repositories ---
        final MemoryContentItemRepository itemRepository = new MemoryContentItemRepository();
        final MemoryContentRevisionRepository revisionRepository = new MemoryContentRevisionRepository();
        final RecordingIndexWriter indexWriter = new RecordingIndexWriter();
        final ContentItemPublishedIndexingSubscriber subscriber =
                new ContentItemPublishedIndexingSubscriber(
                        itemRepository, revisionRepository, indexWriter,
                        new ContentItemIndexDocumentMapper());

        // seed repositories from what the service pipeline created
        itemRepository.save(publishedItem);
        if (publishedItem.currentPublishedRevisionId() != null) {
            // The published revision must be available; reload it via the context runtime is opaque —
            // construct it with the same id the event carries and save into the test repository
            final var revision = codex.codex.api.model.entity.ContentRevision.builder()
                    .id(event.publishedRevisionId())
                    .contentItemId(publishedItem.id())
                    .siteKey(SITE_KEY)
                    .contentTypeKey(CT_KEY)
                    .contentTypeVersionId(publishedItem.contentTypeVersionId())
                    .contentItemKey(ITEM_KEY)
                    .revisionNumber(1)
                    .status(codex.codex.api.model.value.ContentRevisionStatus.PUBLISHED)
                    .values(Map.of(FieldKey.TITLE, "Welcome to Codex"))
                    .createdBy(ACTOR.id())
                    .build();
            revisionRepository.save(revision);
        }

        // --- 6: invoke the subscriber ---
        subscriber.handle(event);

        // --- 7: assert the index was written ---
        assertEquals(1, indexWriter.upserts().size());
        final IndexDocument doc = indexWriter.upserts().getFirst();
        assertEquals(IndexResourceType.CONTENT_ITEM, doc.resourceType());
        assertEquals(SITE_KEY, doc.siteKey());
        assertEquals("content-item:authoring-site:blog-post:welcome-to-codex", doc.id().value());
        assertEquals("Welcome to Codex", doc.title());
    }
}
