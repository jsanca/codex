package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.FieldType;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.tx.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentItemEventPipelineIntegrationTest {

    private static final Actor ACTOR = Actor.system("integration-test");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final FieldKey TITLE_KEY = FieldKey.of("title");

    private TestCodexContext ctx;

    @BeforeEach
    void setUp() throws Exception {
        ctx = TestCodexContext.create();
        // Create and activate a content type with one required title field
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            ctx.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder().key(TITLE_KEY).type(FieldType.TEXT).required(true).build()),
                    ACTOR);
            ctx.contentTypeService().activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });
        ctx.clearEvents();
    }

    // --- 1: inside transaction, before commit ---

    @Test
    @DisplayName("content item create inside transaction does not dispatch before commit")
    void createInsideTransactionDoesNotDispatchBeforeCommit() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                            Map.of(TITLE_KEY, "Hello")),
                    ACTOR);
            ctx.assertNoEvents();
            return null;
        });
    }

    // --- 2: after commit ---

    @Test
    @DisplayName("content item create inside transaction dispatches ContentItemCreatedEvent after commit")
    void createInsideTransactionDispatchesEventAfterCommit() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                            Map.of(TITLE_KEY, "Hello")),
                    ACTOR);
            return null;
        });

        final ContentItemCreatedEvent event = ctx.assertSingleEvent(ContentItemCreatedEvent.class);
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(ContentItemKey.of("my-post"), event.key());
        assertEquals(ACTOR, event.actor());
        assertNotNull(event.contentTypeVersionId());
        assertNotNull(event.occurredAt());
    }

    // --- 3: rollback discards event ---

    @Test
    @DisplayName("content item create rollback does not dispatch event")
    void createRollbackDoesNotDispatchEvent() {
        assertThrows(RuntimeException.class, () ->
                TransactionContext.runInTransaction(() -> {
                    ctx.contentItemService().create(
                            CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                                    Map.of(TITLE_KEY, "Hello")),
                            ACTOR);
                    throw new RuntimeException("forced rollback");
                })
        );

        ctx.assertNoEvents();
    }

    // --- 4: outside transaction dispatches immediately ---

    @Test
    @DisplayName("content item create outside transaction dispatches immediately")
    void createOutsideTransactionDispatchesImmediately() {
        ctx.contentItemService().create(
                CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello")),
                ACTOR);

        ctx.assertSingleEvent(ContentItemCreatedEvent.class);
    }

    // --- 5: duplicate does not dispatch ---

    @Test
    @DisplayName("duplicate content item create does not dispatch event")
    void duplicateCreateDoesNotDispatchEvent() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                            Map.of(TITLE_KEY, "Hello")),
                    ACTOR);
            return null;
        });
        ctx.clearEvents();

        assertThrows(ContentItemAlreadyExistsException.class, () ->
                ctx.contentItemService().create(
                        CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello")),
                        ACTOR));

        ctx.assertNoEvents();
    }

    // --- 6: missing required field does not dispatch ---

    @Test
    @DisplayName("content item create with missing required field does not dispatch event")
    void createWithMissingRequiredFieldDoesNotDispatchEvent() {
        assertThrows(ContentItemFieldValidationException.class, () ->
                ctx.contentItemService().create(
                        CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                                Map.of()),  // title is required but missing
                        ACTOR));

        ctx.assertNoEvents();
    }

    // --- 7: unknown field does not dispatch ---

    @Test
    @DisplayName("content item create with unknown field does not dispatch event")
    void createWithUnknownFieldDoesNotDispatchEvent() {
        assertThrows(ContentItemFieldValidationException.class, () ->
                ctx.contentItemService().create(
                        CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello", FieldKey.of("ghost-field"), "oops")),
                        ACTOR));

        ctx.assertNoEvents();
    }

    // --- 8: non-active content type does not dispatch ---

    @Test
    @DisplayName("content item create for DRAFT content type does not dispatch event")
    void createForDraftContentTypeDoesNotDispatchEvent() throws Exception {
        final ContentTypeKey draftType = ContentTypeKey.of("draft-type");
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, draftType, "Draft Type"), ACTOR);
            return null;
        });
        ctx.clearEvents();

        assertThrows(InvalidContentItemCreationException.class, () ->
                ctx.contentItemService().create(
                        CreateContentItemCommand.of(SITE_KEY, draftType, ContentItemKey.of("some-item"),
                                Map.of()),
                        ACTOR));

        ctx.assertNoEvents();
    }

    // -------------------------------------------------------------------------
    // Publish event pipeline
    // -------------------------------------------------------------------------

    private void createDraftItem() {
        ctx.contentItemService().create(
                CreateContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello")),
                ACTOR);
        ctx.clearEvents();
    }

    @Test
    @DisplayName("publish inside transaction does not dispatch before commit; dispatches ContentItemPublishedEvent after")
    void publishInsideTransactionDispatchesEventAfterCommit() throws Exception {
        createDraftItem();

        TransactionContext.runInTransaction(() -> {
            ctx.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post")),
                    ACTOR);
            ctx.assertNoEventsOfType(ContentItemPublishedEvent.class);
            return null;
        });

        final ContentItemPublishedEvent event = ctx.assertSingleEventOfType(ContentItemPublishedEvent.class);
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(ContentItemKey.of("my-post"), event.key());
        assertEquals(ACTOR, event.actor());
        assertNotNull(event.publishedRevisionId());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("publish rollback does not dispatch ContentItemPublishedEvent")
    void publishRollbackDoesNotDispatchEvent() {
        createDraftItem();

        assertThrows(RuntimeException.class, () ->
                TransactionContext.runInTransaction(() -> {
                    ctx.contentItemService().publish(
                            PublishContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post")),
                            ACTOR);
                    throw new RuntimeException("forced rollback");
                })
        );

        ctx.assertNoEventsOfType(ContentItemPublishedEvent.class);
    }

    @Test
    @DisplayName("publish outside transaction dispatches ContentItemPublishedEvent immediately")
    void publishOutsideTransactionDispatchesImmediately() {
        createDraftItem();

        ctx.contentItemService().publish(
                PublishContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post")),
                ACTOR);

        ctx.assertSingleEventOfType(ContentItemPublishedEvent.class);
    }

    @Test
    @DisplayName("idempotent publish does not dispatch ContentItemPublishedEvent")
    void idempotentPublishDoesNotDispatchEvent() {
        createDraftItem();
        ctx.contentItemService().publish(
                PublishContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post")),
                ACTOR);
        ctx.clearEvents();

        ctx.contentItemService().publish(
                PublishContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("my-post")),
                ACTOR);

        ctx.assertNoEventsOfType(ContentItemPublishedEvent.class);
    }

    @Test
    @DisplayName("publish missing content item does not dispatch ContentItemPublishedEvent")
    void publishMissingItemDoesNotDispatchEvent() {
        assertThrows(Exception.class, () ->
                ctx.contentItemService().publish(
                        PublishContentItemCommand.of(SITE_KEY, CT_KEY, ContentItemKey.of("ghost-post")),
                        ACTOR));

        ctx.assertNoEventsOfType(ContentItemPublishedEvent.class);
    }
}
