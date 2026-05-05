package codex.codex.internal.runtime;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.runtime.CodexRuntime;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.codex.api.model.event.ContentTypeActivatedEvent;
import codex.codex.api.model.event.ContentTypeCreatedEvent;
import codex.codex.api.model.event.SiteCreatedEvent;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.codex.api.model.value.FieldType;
import codex.codex.api.model.value.SiteStatus;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.tx.TransactionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Internal end-to-end integration test for the Codex content authoring flow.
 *
 * <p>Exercises the full in-memory Codex runtime from Site creation through ContentRevision
 * creation, verifying that the domain model, service pipeline, and event dispatching all work
 * together. Does not use HTTP, UI, Spring, or any external infrastructure.
 *
 * <p>Transactional rollback assertions are limited to event dispatching. In-memory repositories
 * are not transactional in this phase; repository-level rollback belongs to a future task.
 */
class ContentAuthoringFlowIntegrationTest {

    private static final Actor ACTOR = Actor.system("integration-test");
    private static final SiteKey SITE_KEY = SiteKey.of("authoring-site");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final FieldKey TITLE_KEY = FieldKey.of("title");
    private static final FieldKey SUMMARY_KEY = FieldKey.of("summary");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("welcome-to-codex");

    // -------------------------------------------------------------------------
    // Full flow — happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("full authoring flow creates content type version content item and working revision")
    void fullAuthoringFlowCreatesContentTypeVersionContentItemAndWorkingRevision() {
        try (final CodexRuntime runtime = CodexRuntime.inMemory()) {

            // 1. Create site
            final Site site = runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Authoring Site"), ACTOR);

            assertNotNull(site);
            assertEquals(SITE_KEY, site.key());
            assertEquals(SiteStatus.STARTED, site.status());

            // 2. Create content type
            final ContentType contentType = runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);

            assertNotNull(contentType);
            assertEquals(ContentTypeStatus.DRAFT, contentType.status());
            assertNull(contentType.latestPublishedVersionId(),
                    "No published version before activation");
            assertNull(contentType.latestPublishedVersion(),
                    "No published version number before activation");

            // 3. Add fields
            final ContentType withTitle = runtime.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder()
                                    .key(TITLE_KEY)
                                    .displayName("Title")
                                    .type(FieldType.TEXT)
                                    .required(true)
                                    .build()),
                    ACTOR);

            runtime.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder()
                                    .key(SUMMARY_KEY)
                                    .displayName("Summary")
                                    .type(FieldType.TEXT)
                                    .required(false)
                                    .build()),
                    ACTOR);

            assertTrue(withTitle.fields().containsKey(TITLE_KEY),
                    "title field should be present after addField");
            assertTrue(withTitle.fields().get(TITLE_KEY).required(),
                    "title field should be required");

            final ContentType afterSummary = runtime.contentTypeService()
                    .findByKey(SITE_KEY, CT_KEY, ACTOR)
                    .orElseThrow(() -> new AssertionError("ContentType not found after addField"));
            assertTrue(afterSummary.fields().containsKey(TITLE_KEY));
            assertTrue(afterSummary.fields().containsKey(SUMMARY_KEY));

            // 4. Activate content type — creates ContentTypeVersion v1
            final ContentType activated = runtime.contentTypeService().activate(
                    ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);

            assertEquals(ContentTypeStatus.ACTIVE, activated.status());
            assertNotNull(activated.latestPublishedVersionId(),
                    "latestPublishedVersionId must be set after activation");
            assertEquals(1, activated.latestPublishedVersion(),
                    "latestPublishedVersion must be 1 after first activation");

            // 5. Create content item
            final ContentItem item = runtime.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(TITLE_KEY, "Welcome to Codex",
                                    SUMMARY_KEY, "A first end-to-end content item")),
                    ACTOR);

            assertNotNull(item);
            assertEquals(ContentItemStatus.DRAFT, item.status());
            assertEquals(ITEM_KEY, item.key());
            assertEquals(SITE_KEY, item.siteKey());
            assertEquals(CT_KEY, item.contentTypeKey());
            assertEquals(activated.latestPublishedVersionId(), item.contentTypeVersionId(),
                    "item must reference the published version");

            // 6. Working revision pointers
            assertNotNull(item.currentWorkingRevisionId(),
                    "currentWorkingRevisionId must be set after creation");
            assertNull(item.currentPublishedRevisionId(),
                    "currentPublishedRevisionId must be null for a new DRAFT item");

            // 7. Publish the content item
            final ContentItem publishedItem = runtime.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);

            assertEquals(ContentItemStatus.PUBLISHED, publishedItem.status());
            assertNotNull(publishedItem.currentPublishedRevisionId(),
                    "currentPublishedRevisionId must be set after publish");
            assertEquals(publishedItem.currentPublishedRevisionId(),
                    publishedItem.currentWorkingRevisionId(),
                    "working and published revision pointers must be equal after first publish");
        }
    }

    // -------------------------------------------------------------------------
    // Transaction behavior — events dispatched only after commit
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("full authoring flow dispatches events only after commit")
    void fullAuthoringFlowDispatchesEventsOnlyAfterCommit() throws Exception {
        try (final CodexRuntime runtime = CodexRuntime.inMemory()) {

            TransactionContext.runInTransaction(() -> {
                performFullAuthoringFlow(runtime);

                assertTrue(runtime.recordedEvents().isEmpty(),
                        "No events must be dispatched before transaction commits");
                return null;
            });

            final List<CodexEvent> events = runtime.recordedEvents();
            assertFalse(events.isEmpty(), "Events must be dispatched after commit");

            assertEventPresent(events, SiteCreatedEvent.class);
            assertEventPresent(events, ContentTypeCreatedEvent.class);
            assertEventPresent(events, ContentTypeActivatedEvent.class);
            assertEventPresent(events, ContentItemCreatedEvent.class);
        }
    }

    // -------------------------------------------------------------------------
    // Transaction behavior — rollback discards events
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("full authoring flow rollback dispatches no events")
    void fullAuthoringFlowRollbackDispatchesNoEvents() {
        try (final CodexRuntime runtime = CodexRuntime.inMemory()) {

            assertThrows(RuntimeException.class, () ->
                    TransactionContext.runInTransaction(() -> {
                        performFullAuthoringFlow(runtime);
                        throw new RuntimeException("forced rollback");
                    })
            );

            assertTrue(runtime.recordedEvents().isEmpty(),
                    "No events must be dispatched after rollback");
        }
    }

    // -------------------------------------------------------------------------
    // Validation — missing required field fails fast
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("content item creation fails when required field is missing in full flow")
    void contentItemCreationFailsWhenRequiredFieldIsMissing() {
        try (final CodexRuntime runtime = CodexRuntime.inMemory()) {
            setupActiveContentType(runtime);
            runtime.clearRecordedEvents();

            assertThrows(Exception.class, () ->
                    runtime.contentItemService().create(
                            CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                                    Map.of(SUMMARY_KEY, "No title provided")),  // title is required but missing
                            ACTOR)
            );

            assertTrue(runtime.recordedEvents().isEmpty(),
                    "No ContentItemCreatedEvent must be dispatched when creation fails");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void performFullAuthoringFlow(final CodexRuntime runtime) {
        runtime.siteService().create(
                CreateSiteCommand.of(SITE_KEY, "Authoring Site"), ACTOR);

        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);

        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                        Field.builder().key(TITLE_KEY).displayName("Title")
                                .type(FieldType.TEXT).required(true).build()),
                ACTOR);

        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                        Field.builder().key(SUMMARY_KEY).displayName("Summary")
                                .type(FieldType.TEXT).required(false).build()),
                ACTOR);

        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);

        runtime.contentItemService().create(
                CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                        Map.of(TITLE_KEY, "Welcome to Codex",
                                SUMMARY_KEY, "A first end-to-end content item")),
                ACTOR);
    }

    private void setupActiveContentType(final CodexRuntime runtime) {
        runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Authoring Site"), ACTOR);
        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Blog Post"), ACTOR);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                        Field.builder().key(TITLE_KEY).displayName("Title")
                                .type(FieldType.TEXT).required(true).build()),
                ACTOR);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                        Field.builder().key(SUMMARY_KEY).displayName("Summary")
                                .type(FieldType.TEXT).required(false).build()),
                ACTOR);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
    }

    private <E extends CodexEvent> void assertEventPresent(
            final List<CodexEvent> events, final Class<E> type) {
        assertTrue(events.stream().anyMatch(type::isInstance),
                "Expected event of type " + type.getSimpleName() + " but was not found in: " + events);
    }
}
