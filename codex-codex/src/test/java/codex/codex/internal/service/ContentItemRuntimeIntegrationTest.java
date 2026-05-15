package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.FieldType;
import codex.codex.api.runtime.CodexRuntime;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

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
        assertNotNull(item.currentWorkingRevisionId());
        assertNull(item.currentPublishedRevisionId());
        assertEquals(actor.id(), item.owner());
        assertEquals(actor.id(), item.createdBy());
    }

    /**
     * Verifies that the cache invalidation subscriber for ContentItemCreatedEvent is wired
     * into the runtime event pipeline.
     *
     * <p>Flow: a {@code findByKey} for a non-existent item produces a cached {@code NotFound}
     * entry. Creating the item fires a {@code ContentItemCreatedEvent} which the wired
     * {@code ContentItemCreatedCacheInvalidationSubscriber} evicts. A subsequent
     * {@code findByKey} must reach the delegate and return the real item.</p>
     */
    @Test
    void cacheNegativeEntryIsEvictedWhenItemIsCreated() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("cache-test-post");
        final FieldKey titleKey = FieldKey.of("title");

        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(siteKey, contentTypeKey, "Blog Post"), actor);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(siteKey, contentTypeKey,
                        Field.builder().key(titleKey).type(FieldType.TEXT).required(true).build()),
                actor);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(siteKey, contentTypeKey), actor);

        // First read — item does not exist; result is cached as NotFound.
        final Optional<ContentItem> beforeCreate =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(beforeCreate.isEmpty(), "item must not exist before creation");

        // Create the item — ContentItemCreatedEvent is dispatched synchronously,
        // ContentItemCreatedCacheInvalidationSubscriber evicts the NotFound entry.
        runtime.contentItemService().create(
                CreateContentItemCommand.of(siteKey, contentTypeKey, itemKey,
                        Map.of(titleKey, "Cache Test Post")),
                actor);

        // Second read — cache entry was evicted, so the delegate is consulted and returns the item.
        final Optional<ContentItem> afterCreate =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(afterCreate.isPresent(),
                "item must be found after cache invalidation driven by ContentItemCreatedEvent");
    }

    /**
     * Verifies that the cache invalidation subscriber for ContentItemUpdatedEvent is wired
     * into the runtime event pipeline.
     *
     * <p>Flow: create an item, load it to populate the cache (positive entry), update it which
     * fires a {@code ContentItemUpdatedEvent} and evicts the cached entry. A subsequent
     * {@code findByKey} must reload from the delegate and return the refreshed item.</p>
     */
    @Test
    void cacheEntryIsEvictedWhenItemIsUpdated() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("update-cache-test");
        final FieldKey titleKey = FieldKey.of("title");

        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(siteKey, contentTypeKey, "Blog Post"), actor);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(siteKey, contentTypeKey,
                        Field.builder().key(titleKey).type(FieldType.TEXT).required(true).build()),
                actor);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(siteKey, contentTypeKey), actor);

        runtime.contentItemService().create(
                CreateContentItemCommand.of(siteKey, contentTypeKey, itemKey,
                        Map.of(titleKey, "Original Title")),
                actor);

        // Load the item — populates the cache with a positive entry.
        final Optional<ContentItem> beforeUpdate =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(beforeUpdate.isPresent(), "item must exist before update");

        // Update the item — ContentItemUpdatedEvent is dispatched synchronously,
        // ContentItemUpdatedCacheInvalidationSubscriber evicts the cache entry.
        runtime.contentItemService().update(
                UpdateContentItemCommand.of(siteKey, contentTypeKey, itemKey,
                        Map.of(titleKey, "Updated Title")),
                actor);

        // Second read — cache entry was evicted, so the delegate is consulted and returns the item.
        final Optional<ContentItem> afterUpdate =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(afterUpdate.isPresent(),
                "item must be found after cache invalidation driven by ContentItemUpdatedEvent");
    }

    /**
     * Verifies that the cache invalidation subscriber for ContentItemUnpublishedEvent is wired
     * into the runtime event pipeline.
     *
     * <p>Flow: create and publish an item, load it to populate the cache, then unpublish it
     * which fires a {@code ContentItemUnpublishedEvent} and evicts the cached entry. A subsequent
     * {@code findByKey} must reload from the delegate and return the updated (DRAFT) item.</p>
     */
    @Test
    void cacheEntryIsEvictedWhenItemIsUnpublished() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("unpublish-cache-test");
        final FieldKey titleKey = FieldKey.of("title");

        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(siteKey, contentTypeKey, "Blog Post"), actor);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(siteKey, contentTypeKey,
                        Field.builder().key(titleKey).type(FieldType.TEXT).required(true).build()),
                actor);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(siteKey, contentTypeKey), actor);

        runtime.contentItemService().create(
                CreateContentItemCommand.of(siteKey, contentTypeKey, itemKey,
                        Map.of(titleKey, "To Be Unpublished")),
                actor);
        runtime.contentItemService().publish(
                PublishContentItemCommand.of(siteKey, contentTypeKey, itemKey), actor);

        // Load the published item — populates the cache with a positive entry.
        final Optional<ContentItem> beforeUnpublish =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(beforeUnpublish.isPresent(), "item must be published before unpublish");

        // Unpublish — ContentItemUnpublishedEvent is dispatched synchronously,
        // ContentItemUnpublishedCacheInvalidationSubscriber evicts the cache entry.
        runtime.contentItemService().unpublish(
                UnpublishContentItemCommand.of(siteKey, contentTypeKey, itemKey), actor);

        // Second read — cache entry was evicted, so the delegate is consulted and returns the item.
        final Optional<ContentItem> afterUnpublish =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(afterUnpublish.isPresent(),
                "item must be found after cache invalidation driven by ContentItemUnpublishedEvent");
        assertEquals(ContentItemStatus.DRAFT, afterUnpublish.get().status(),
                "item must be in DRAFT status after unpublish");
    }

    /**
     * Verifies that the cache invalidation subscriber for ContentItemArchivedEvent is wired
     * into the runtime event pipeline.
     *
     * <p>Flow: create an item, load it to populate the cache (positive entry), then archive it
     * which fires a {@code ContentItemArchivedEvent} and evicts the cached entry. A subsequent
     * {@code findByKey} must reload from the delegate and return the updated (ARCHIVED) item.</p>
     */
    @Test
    void cacheEntryIsEvictedWhenItemIsArchived() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("archive-cache-test");
        final FieldKey titleKey = FieldKey.of("title");

        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(siteKey, contentTypeKey, "Blog Post"), actor);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(siteKey, contentTypeKey,
                        Field.builder().key(titleKey).type(FieldType.TEXT).required(true).build()),
                actor);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(siteKey, contentTypeKey), actor);

        runtime.contentItemService().create(
                CreateContentItemCommand.of(siteKey, contentTypeKey, itemKey,
                        Map.of(titleKey, "To Be Archived")),
                actor);

        // Load the item — populates the cache with a positive entry.
        final Optional<ContentItem> beforeArchive =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(beforeArchive.isPresent(), "item must exist before archive");

        // Archive — ContentItemArchivedEvent is dispatched synchronously,
        // ContentItemArchivedCacheInvalidationSubscriber evicts the cache entry.
        runtime.contentItemService().archive(
                ArchiveContentItemCommand.of(siteKey, contentTypeKey, itemKey), actor);

        // Second read — cache entry was evicted, so the delegate is consulted and returns the item.
        final Optional<ContentItem> afterArchive =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(afterArchive.isPresent(),
                "item must be found after cache invalidation driven by ContentItemArchivedEvent");
        assertEquals(ContentItemStatus.ARCHIVED, afterArchive.get().status(),
                "item must be in ARCHIVED status after archive");
    }

    /**
     * Verifies that the cache invalidation subscriber for ContentItemRestoredEvent is wired
     * into the runtime event pipeline.
     *
     * <p>Flow: create and archive an item, load it to populate the cache (positive ARCHIVED entry),
     * then restore it which fires a {@code ContentItemRestoredEvent} and evicts the cached entry.
     * A subsequent {@code findByKey} must reload from the delegate and return the restored (DRAFT)
     * item.</p>
     */
    @Test
    void cacheEntryIsEvictedWhenItemIsRestored() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("restore-cache-test");
        final FieldKey titleKey = FieldKey.of("title");

        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(siteKey, contentTypeKey, "Blog Post"), actor);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(siteKey, contentTypeKey,
                        Field.builder().key(titleKey).type(FieldType.TEXT).required(true).build()),
                actor);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(siteKey, contentTypeKey), actor);

        runtime.contentItemService().create(
                CreateContentItemCommand.of(siteKey, contentTypeKey, itemKey,
                        Map.of(titleKey, "To Be Restored")),
                actor);
        runtime.contentItemService().archive(
                ArchiveContentItemCommand.of(siteKey, contentTypeKey, itemKey), actor);

        // Load the archived item — populates the cache with a positive entry.
        final Optional<ContentItem> beforeRestore =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(beforeRestore.isPresent(), "item must exist in ARCHIVED state before restore");
        assertEquals(ContentItemStatus.ARCHIVED, beforeRestore.get().status());

        // Restore — ContentItemRestoredEvent is dispatched synchronously,
        // ContentItemRestoredCacheInvalidationSubscriber evicts the cache entry.
        runtime.contentItemService().restore(
                RestoreContentItemCommand.of(siteKey, contentTypeKey, itemKey), actor);

        // Second read — cache entry was evicted, so the delegate is consulted and returns the item.
        final Optional<ContentItem> afterRestore =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(afterRestore.isPresent(),
                "item must be found after cache invalidation driven by ContentItemRestoredEvent");
        assertEquals(ContentItemStatus.DRAFT, afterRestore.get().status(),
                "item must be in DRAFT status after restore");
    }

    /**
     * Verifies that the cache invalidation subscriber for ContentItemDeletedEvent is wired
     * into the runtime event pipeline.
     *
     * <p>Flow: create and archive an item, load it to populate the cache (positive entry),
     * then delete it which fires a {@code ContentItemDeletedEvent} and evicts the cached entry.
     * A subsequent {@code findByKey} must return empty — both because the item is gone and
     * because the cache entry was evicted.</p>
     */
    @Test
    void cacheEntryIsEvictedWhenItemIsDeleted() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("delete-cache-test");
        final FieldKey titleKey = FieldKey.of("title");

        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(siteKey, contentTypeKey, "Blog Post"), actor);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(siteKey, contentTypeKey,
                        Field.builder().key(titleKey).type(FieldType.TEXT).required(true).build()),
                actor);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(siteKey, contentTypeKey), actor);

        runtime.contentItemService().create(
                CreateContentItemCommand.of(siteKey, contentTypeKey, itemKey,
                        Map.of(titleKey, "To Be Deleted")),
                actor);
        runtime.contentItemService().archive(
                ArchiveContentItemCommand.of(siteKey, contentTypeKey, itemKey), actor);

        // Load the archived item — populates the cache with a positive entry.
        final Optional<ContentItem> beforeDelete =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(beforeDelete.isPresent(), "item must exist in ARCHIVED state before delete");

        // Delete — ContentItemDeletedEvent is dispatched synchronously,
        // ContentItemDeletedCacheInvalidationSubscriber evicts the cache entry.
        runtime.contentItemService().delete(
                DeleteContentItemCommand.of(siteKey, contentTypeKey, itemKey), actor);

        // Second read — cache entry was evicted and item is gone; must return empty.
        final Optional<ContentItem> afterDelete =
                runtime.contentItemService().findByKey(siteKey, contentTypeKey, itemKey, actor);
        assertTrue(afterDelete.isEmpty(),
                "item must not be found after hard delete and cache invalidation");
    }
}
