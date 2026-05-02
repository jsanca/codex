package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.value.FieldType;
import codex.codex.api.model.event.ContentTypeActivatedEvent;
import codex.codex.api.model.event.ContentTypeArchivedEvent;
import codex.codex.api.model.event.ContentTypeCreatedEvent;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.tx.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentTypeEventPipelineIntegrationTest {

    private static final Actor ACTOR = Actor.system("integration-test");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");

    private TestCodexContext ctx;

    @BeforeEach
    void setUp() {
        ctx = TestCodexContext.create();
    }

    // --- create ---

    @Test
    @DisplayName("content type create inside transaction does not dispatch before commit")
    void createInsideTransactionDoesNotDispatchBeforeCommit() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            ctx.assertNoEvents();
            return null;
        });
    }

    @Test
    @DisplayName("content type create inside transaction dispatches ContentTypeCreatedEvent after commit")
    void createInsideTransactionDispatchesCreatedEventAfterCommit() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });

        final ContentTypeCreatedEvent event = ctx.assertSingleEvent(ContentTypeCreatedEvent.class);
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("content type create rollback does not dispatch event")
    void createRollbackDoesNotDispatchEvent() {
        assertThrows(RuntimeException.class, () ->
                TransactionContext.runInTransaction(() -> {
                    ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
                    throw new RuntimeException("forced rollback");
                })
        );

        ctx.assertNoEvents();
    }

    // --- activate ---

    @Test
    @DisplayName("content type activate dispatches ContentTypeActivatedEvent only when status changes")
    void activateDispatchesActivatedEventOnlyWhenStatusChanges() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });
        ctx.clearEvents();

        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });

        final ContentTypeActivatedEvent event = ctx.assertSingleEvent(ContentTypeActivatedEvent.class);
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("idempotent content type activate does not dispatch event")
    void idempotentActivateDoesNotDispatchEvent() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });
        ctx.clearEvents();

        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });

        ctx.assertNoEvents();
    }

    @Test
    @DisplayName("rollback after content type activate does not dispatch event")
    void rollbackAfterActivateDoesNotDispatchEvent() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });
        ctx.clearEvents();

        assertThrows(RuntimeException.class, () ->
                TransactionContext.runInTransaction(() -> {
                    ctx.contentTypeService().activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
                    throw new RuntimeException("forced rollback");
                })
        );

        ctx.assertNoEvents();
    }

    // --- archive ---

    @Test
    @DisplayName("content type archive dispatches ContentTypeArchivedEvent only when status changes")
    void archiveDispatchesArchivedEventOnlyWhenStatusChanges() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });
        ctx.clearEvents();

        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().archive(ArchiveContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });

        final ContentTypeArchivedEvent event = ctx.assertSingleEvent(ContentTypeArchivedEvent.class);
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("idempotent content type archive does not dispatch event")
    void idempotentArchiveDoesNotDispatchEvent() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().archive(ArchiveContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });
        ctx.clearEvents();

        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().archive(ArchiveContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });

        ctx.assertNoEvents();
    }

    @Test
    @DisplayName("rollback after content type archive does not dispatch event")
    void rollbackAfterArchiveDoesNotDispatchEvent() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });
        ctx.clearEvents();

        assertThrows(RuntimeException.class, () ->
                TransactionContext.runInTransaction(() -> {
                    ctx.contentTypeService().archive(ArchiveContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
                    throw new RuntimeException("forced rollback");
                })
        );

        ctx.assertNoEvents();
    }

    // --- invalid transition ---

    @Test
    @DisplayName("invalid content type transition does not dispatch event")
    void invalidTransitionDoesNotDispatchEvent() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().archive(ArchiveContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
            return null;
        });
        ctx.clearEvents();

        assertThrows(InvalidContentTypeStatusTransitionException.class, () ->
                TransactionContext.runInTransaction(() -> {
                    ctx.contentTypeService().activate(ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
                    return null;
                })
        );

        ctx.assertNoEvents();
    }

    // --- outside transaction ---

    @Test
    @DisplayName("content type outside transaction dispatches immediately")
    void outsideTransactionDispatchesImmediately() {
        ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
        ctx.assertSingleEvent(ContentTypeCreatedEvent.class);
    }

    // --- field operations ---

    @Test
    @DisplayName("create, add field, and remove field work end-to-end")
    void createAddFieldAndRemoveFieldWorkEndToEnd() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.contentTypeService().create(CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
            return null;
        });

        final FieldKey titleKey = FieldKey.of("title");
        final Field titleField = Field.builder().key(titleKey).type(FieldType.TEXT).displayName("Title").build();

        final codex.codex.api.model.entity.ContentType withField = ctx.contentTypeService()
                .addField(AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY, titleField), ACTOR);
        assertEquals(1, withField.fields().size());
        assertEquals(titleField, withField.fields().get(titleKey));

        final codex.codex.api.model.entity.ContentType withoutField = ctx.contentTypeService()
                .removeField(RemoveContentTypeFieldCommand.of(SITE_KEY, CT_KEY, titleKey), ACTOR);
        assertTrue(withoutField.fields().isEmpty());
    }
}
