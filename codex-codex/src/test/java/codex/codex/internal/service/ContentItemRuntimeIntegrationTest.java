package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.FieldType;
import codex.codex.internal.runtime.CodexRuntime;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying the full content item creation pipeline
 * through {@link CodexRuntime}.
 */
class ContentItemRuntimeIntegrationTest {

    private CodexRuntime runtime;
    private final Actor actor = Actor.system("integration-test");

    @BeforeEach
    void setUp() {
        runtime = CodexRuntime.inMemory();
    }

    @AfterEach
    void tearDown() {
        runtime.shutdown();
    }

    @Test
    void createContentItemThroughFullPipeline() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final FieldKey titleKey = FieldKey.of("title");

        // Create content type
        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(siteKey, contentTypeKey, "Blog Post"),
                actor);

        // Add required title field
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(siteKey, contentTypeKey,
                        Field.builder().key(titleKey).type(FieldType.TEXT).required(true).build()),
                actor);

        // Activate to publish a version
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(siteKey, contentTypeKey),
                actor);

        // Create content item
        final ContentItem item = runtime.contentItemService().create(
                CreateContentItemCommand.of(siteKey, contentTypeKey,
                        ContentItemKey.of("welcome-to-codex"),
                        Map.of(titleKey, "Welcome to Codex")),
                actor);

        // Verify item was created as draft with the correct version reference
        assertNotNull(item);
        assertEquals(ContentItemStatus.DRAFT, item.status());
        assertEquals(siteKey, item.siteKey());
        assertEquals(contentTypeKey, item.contentTypeKey());
        assertNotNull(item.contentTypeVersionId());
        assertEquals("Welcome to Codex", item.values().get(titleKey));
        assertEquals(actor.id(), item.owner());
        assertEquals(actor.id(), item.createdBy());
    }
}
