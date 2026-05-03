package codex.codex.internal.service;

import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.entity.ContentTypeVersion;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.codex.api.model.value.ContentTypeVersionStatus;
import codex.codex.api.model.value.FieldType;
import codex.codex.internal.repository.MemoryContentItemRepository;
import codex.codex.internal.repository.MemoryContentRevisionRepository;
import codex.codex.internal.repository.MemoryContentTypeRepository;
import codex.codex.internal.repository.MemoryContentTypeVersionRepository;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CodexContentItemService}.
 */
class CodexContentItemServiceTest {

    private static final SiteKey SITE_ACME = SiteKey.of("acme");
    private static final SiteKey SITE_BETA = SiteKey.of("beta");
    private static final ContentTypeKey BLOG_POST = ContentTypeKey.of("blog-post");
    private static final ContentTypeKey PRODUCT = ContentTypeKey.of("product");
    private static final FieldKey TITLE_KEY = FieldKey.of("title");
    private static final FieldKey BODY_KEY = FieldKey.of("body");
    private static final Actor ACTOR = Actor.system("test");

    private MemoryContentItemRepository itemRepository;
    private MemoryContentRevisionRepository revisionRepository;
    private MemoryContentTypeRepository contentTypeRepository;
    private MemoryContentTypeVersionRepository versionRepository;
    private CodexContentItemService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        itemRepository = new MemoryContentItemRepository();
        revisionRepository = new MemoryContentRevisionRepository();
        contentTypeRepository = new MemoryContentTypeRepository();
        versionRepository = new MemoryContentTypeVersionRepository();
        clock = Clock.fixed(Instant.parse("2025-06-01T10:00:00Z"), ZoneOffset.UTC);
        service = new CodexContentItemService(itemRepository, revisionRepository, contentTypeRepository, versionRepository, clock);
    }

    /**
     * Sets up an ACTIVE content type with one required title field and one optional body field,
     * and a published ContentTypeVersion for that content type.
     */
    private ContentTypeVersionId setupActiveContentType(final SiteKey siteKey, final ContentTypeKey contentTypeKey) {
        final ContentTypeId contentTypeId = ContentTypeId.of("ct-" + siteKey.value() + "-" + contentTypeKey.value());
        final ContentTypeVersionId versionId = ContentTypeVersionId.forVersion(siteKey, contentTypeKey, 1);

        final Field titleField = Field.builder()
                .key(TITLE_KEY)
                .displayName("Title")
                .type(FieldType.TEXT)
                .required(true)
                .build();

        final Field bodyField = Field.builder()
                .key(BODY_KEY)
                .displayName("Body")
                .type(FieldType.TEXT)
                .required(false)
                .build();

        final ContentTypeVersion version = ContentTypeVersion.builder()
                .id(versionId)
                .contentTypeId(contentTypeId)
                .siteKey(siteKey)
                .contentTypeKey(contentTypeKey)
                .version(1)
                .fields(Map.of(TITLE_KEY, titleField, BODY_KEY, bodyField))
                .status(ContentTypeVersionStatus.PUBLISHED)
                .createdBy(ACTOR.id())
                .createdAt(clock.instant())
                .build();

        final ContentType contentType = ContentType.builder()
                .id(contentTypeId)
                .siteKey(siteKey)
                .key(contentTypeKey)
                .displayName("Blog Post")
                .status(ContentTypeStatus.ACTIVE)
                .latestPublishedVersionId(versionId)
                .latestPublishedVersion(1)
                .owner(ACTOR.id())
                .createdBy(ACTOR.id())
                .updatedBy(ACTOR.id())
                .createdAt(clock.instant())
                .updatedAt(clock.instant())
                .build();

        contentTypeRepository.save(contentType);
        versionRepository.save(version);
        return versionId;
    }

    @Test
    void createShouldCreateDraftContentItem() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItem item = service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello World")),
                ACTOR);

        assertEquals(ContentItemStatus.DRAFT, item.status());
    }

    @Test
    void createShouldSetSiteKey() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItem item = service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello")),
                ACTOR);

        assertEquals(SITE_ACME, item.siteKey());
    }

    @Test
    void createShouldSetContentTypeKey() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItem item = service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello")),
                ACTOR);

        assertEquals(BLOG_POST, item.contentTypeKey());
    }

    @Test
    void createShouldSetContentTypeVersionIdFromLatestPublished() {
        final ContentTypeVersionId expectedVersionId = setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItem item = service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello")),
                ACTOR);

        assertEquals(expectedVersionId, item.contentTypeVersionId());
    }

    @Test
    void createShouldSetOwnerFromActorId() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItem item = service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello")),
                ACTOR);

        assertEquals(ACTOR.id(), item.owner());
    }

    @Test
    void createShouldSetCreatedByFromActorId() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItem item = service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello")),
                ACTOR);

        assertEquals(ACTOR.id(), item.createdBy());
    }

    @Test
    void createShouldSetUpdatedByFromActorId() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItem item = service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello")),
                ACTOR);

        assertEquals(ACTOR.id(), item.updatedBy());
    }

    @Test
    void createShouldPreserveProvidedFieldValuesInFirstRevision() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItem item = service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello World", BODY_KEY, "Content body here")),
                ACTOR);

        assertNotNull(item.currentWorkingRevisionId());
        final codex.codex.api.model.entity.ContentRevision revision = revisionRepository.findById(item.currentWorkingRevisionId()).orElseThrow();
        assertEquals("Hello World", revision.values().get(TITLE_KEY));
        assertEquals("Content body here", revision.values().get(BODY_KEY));
        assertEquals(1, revision.revisionNumber());
        assertEquals(codex.codex.api.model.value.ContentRevisionStatus.WORKING, revision.status());
        assertEquals(item.id(), revision.contentItemId());
        assertEquals(item.contentTypeVersionId(), revision.contentTypeVersionId());
        assertEquals(ACTOR.id(), revision.createdBy());
    }

    @Test
    void createShouldRejectDuplicateItemKeyInSameSiteAndContentType() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final CreateContentItemCommand command = CreateContentItemCommand.of(
                SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"), Map.of(TITLE_KEY, "Hello"));

        service.create(command, ACTOR);

        assertThrows(ContentItemAlreadyExistsException.class,
                () -> service.create(command, ACTOR));
    }

    @Test
    void createShouldAllowSameItemKeyUnderDifferentContentType() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        setupActiveContentType(SITE_ACME, PRODUCT);

        service.create(CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("item-one"),
                Map.of(TITLE_KEY, "Hello")), ACTOR);
        assertDoesNotThrow(() -> service.create(
                CreateContentItemCommand.of(SITE_ACME, PRODUCT, ContentItemKey.of("item-one"),
                        Map.of(TITLE_KEY, "Hello")), ACTOR));
    }

    @Test
    void createShouldAllowSameItemKeyUnderDifferentSite() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        setupActiveContentType(SITE_BETA, BLOG_POST);

        service.create(CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("item-one"),
                Map.of(TITLE_KEY, "Hello")), ACTOR);
        assertDoesNotThrow(() -> service.create(
                CreateContentItemCommand.of(SITE_BETA, BLOG_POST, ContentItemKey.of("item-one"),
                        Map.of(TITLE_KEY, "Hello")), ACTOR));
    }

    @Test
    void createShouldRejectMissingContentType() {
        assertThrows(NotFoundException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello")),
                        ACTOR));
    }

    @Test
    void createShouldRejectDraftContentType() {
        final ContentTypeId contentTypeId = ContentTypeId.of("ct-draft");
        contentTypeRepository.save(ContentType.builder()
                .id(contentTypeId)
                .siteKey(SITE_ACME)
                .key(BLOG_POST)
                .displayName("Blog Post")
                .status(ContentTypeStatus.DRAFT)
                .owner(ACTOR.id())
                .createdBy(ACTOR.id())
                .updatedBy(ACTOR.id())
                .createdAt(clock.instant())
                .updatedAt(clock.instant())
                .build());

        assertThrows(InvalidContentItemCreationException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello")),
                        ACTOR));
    }

    @Test
    void createShouldRejectArchivedContentType() {
        final ContentTypeId contentTypeId = ContentTypeId.of("ct-archived");
        contentTypeRepository.save(ContentType.builder()
                .id(contentTypeId)
                .siteKey(SITE_ACME)
                .key(BLOG_POST)
                .displayName("Blog Post")
                .status(ContentTypeStatus.ARCHIVED)
                .owner(ACTOR.id())
                .createdBy(ACTOR.id())
                .updatedBy(ACTOR.id())
                .createdAt(clock.instant())
                .updatedAt(clock.instant())
                .build());

        assertThrows(InvalidContentItemCreationException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello")),
                        ACTOR));
    }

    @Test
    void createShouldRejectContentTypeWithoutLatestPublishedVersionId() {
        final ContentTypeId contentTypeId = ContentTypeId.of("ct-active-no-version");
        contentTypeRepository.save(ContentType.builder()
                .id(contentTypeId)
                .siteKey(SITE_ACME)
                .key(BLOG_POST)
                .displayName("Blog Post")
                .status(ContentTypeStatus.ACTIVE)
                // no latestPublishedVersionId
                .owner(ACTOR.id())
                .createdBy(ACTOR.id())
                .updatedBy(ACTOR.id())
                .createdAt(clock.instant())
                .updatedAt(clock.instant())
                .build());

        assertThrows(InvalidContentItemCreationException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello")),
                        ACTOR));
    }

    @Test
    void createShouldRejectMissingContentTypeVersion() {
        final ContentTypeId contentTypeId = ContentTypeId.of("ct-ghost-version");
        final ContentTypeVersionId ghostVersionId = ContentTypeVersionId.of("ghost-version-id");
        contentTypeRepository.save(ContentType.builder()
                .id(contentTypeId)
                .siteKey(SITE_ACME)
                .key(BLOG_POST)
                .displayName("Blog Post")
                .status(ContentTypeStatus.ACTIVE)
                .latestPublishedVersionId(ghostVersionId)
                .latestPublishedVersion(1)
                .owner(ACTOR.id())
                .createdBy(ACTOR.id())
                .updatedBy(ACTOR.id())
                .createdAt(clock.instant())
                .updatedAt(clock.instant())
                .build());
        // version is NOT saved to versionRepository

        assertThrows(NotFoundException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello")),
                        ACTOR));
    }

    @Test
    void createShouldRejectNonPublishedContentTypeVersion() {
        final ContentTypeId contentTypeId = ContentTypeId.of("ct-deprecated-version");
        final ContentTypeVersionId versionId = ContentTypeVersionId.of("deprecated-version-id");

        final Field titleField = Field.builder().key(TITLE_KEY).type(FieldType.TEXT).required(true).build();
        final ContentTypeVersion deprecatedVersion = ContentTypeVersion.builder()
                .id(versionId)
                .contentTypeId(contentTypeId)
                .siteKey(SITE_ACME)
                .contentTypeKey(BLOG_POST)
                .version(1)
                .fields(Map.of(TITLE_KEY, titleField))
                .status(ContentTypeVersionStatus.DEPRECATED)
                .createdBy(ACTOR.id())
                .createdAt(clock.instant())
                .build();

        contentTypeRepository.save(ContentType.builder()
                .id(contentTypeId)
                .siteKey(SITE_ACME)
                .key(BLOG_POST)
                .displayName("Blog Post")
                .status(ContentTypeStatus.ACTIVE)
                .latestPublishedVersionId(versionId)
                .latestPublishedVersion(1)
                .owner(ACTOR.id())
                .createdBy(ACTOR.id())
                .updatedBy(ACTOR.id())
                .createdAt(clock.instant())
                .updatedAt(clock.instant())
                .build());
        versionRepository.save(deprecatedVersion);

        assertThrows(InvalidContentItemCreationException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello")),
                        ACTOR));
    }

    @Test
    void createShouldRejectUnknownFieldValues() {
        setupActiveContentType(SITE_ACME, BLOG_POST);

        assertThrows(ContentItemFieldValidationException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello", FieldKey.of("nonexistent"), "oops")),
                        ACTOR));
    }

    @Test
    void createShouldRejectMissingRequiredField() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        // title is required but not provided
        assertThrows(ContentItemFieldValidationException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(BODY_KEY, "some body")),
                        ACTOR));
    }

    @Test
    void createShouldAllowMissingOptionalField() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        // body is optional — providing only title is valid
        assertDoesNotThrow(() -> service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello World")),
                ACTOR));
    }

    @Test
    void findByKeyShouldReturnExistingItem() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        final ContentItemKey key = ContentItemKey.of("my-post");
        service.create(CreateContentItemCommand.of(SITE_ACME, BLOG_POST, key, Map.of(TITLE_KEY, "Hello")), ACTOR);

        final Optional<ContentItem> found = service.findByKey(SITE_ACME, BLOG_POST, key, ACTOR);
        assertTrue(found.isPresent());
        assertEquals(key, found.get().key());
    }

    @Test
    void findByKeyShouldReturnEmptyWhenMissing() {
        final Optional<ContentItem> found = service.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("ghost"), ACTOR);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByContentTypeShouldReturnOnlyMatchingItems() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        setupActiveContentType(SITE_ACME, PRODUCT);

        service.create(CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("post-one"),
                Map.of(TITLE_KEY, "Post One")), ACTOR);
        service.create(CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("post-two"),
                Map.of(TITLE_KEY, "Post Two")), ACTOR);
        service.create(CreateContentItemCommand.of(SITE_ACME, PRODUCT, ContentItemKey.of("prod-one"),
                Map.of(TITLE_KEY, "Product One")), ACTOR);

        final List<ContentItem> results = service.findByContentType(SITE_ACME, BLOG_POST, ACTOR);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(item -> item.contentTypeKey().equals(BLOG_POST)));
    }

    @Test
    void findAllShouldReturnAllItems() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        setupActiveContentType(SITE_BETA, PRODUCT);

        service.create(CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("post-one"),
                Map.of(TITLE_KEY, "Post One")), ACTOR);
        service.create(CreateContentItemCommand.of(SITE_BETA, PRODUCT, ContentItemKey.of("prod-one"),
                Map.of(TITLE_KEY, "Product One")), ACTOR);

        final List<ContentItem> all = service.findAll(ACTOR);
        assertEquals(2, all.size());
    }

    @Test
    void createRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.create(null, ACTOR));
    }

    @Test
    void createRejectsNullActor() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        assertThrows(NullPointerException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                                Map.of(TITLE_KEY, "Hello")),
                        null));
    }

    @Test
    void findByKeyRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> service.findByKey(null, BLOG_POST, ContentItemKey.of("post"), ACTOR));
        assertThrows(NullPointerException.class,
                () -> service.findByKey(SITE_ACME, null, ContentItemKey.of("post"), ACTOR));
        assertThrows(NullPointerException.class,
                () -> service.findByKey(SITE_ACME, BLOG_POST, null, ACTOR));
        assertThrows(NullPointerException.class,
                () -> service.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("post"), null));
    }

    @Test
    void findByContentTypeRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> service.findByContentType(null, BLOG_POST, ACTOR));
        assertThrows(NullPointerException.class,
                () -> service.findByContentType(SITE_ACME, null, ACTOR));
        assertThrows(NullPointerException.class,
                () -> service.findByContentType(SITE_ACME, BLOG_POST, null));
    }

    @Test
    void findAllRejectsNullActor() {
        assertThrows(NullPointerException.class, () -> service.findAll(null));
    }

    // -------------------------------------------------------------------------
    // publish
    // -------------------------------------------------------------------------

    private ContentItem createDraftItem() {
        setupActiveContentType(SITE_ACME, BLOG_POST);
        return service.create(
                CreateContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"),
                        Map.of(TITLE_KEY, "Hello World", BODY_KEY, "Body text")),
                ACTOR);
    }

    @Test
    void publishShouldUpdateItemStatusToPublished() {
        final ContentItem draft = createDraftItem();
        final ContentItem published = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        assertEquals(ContentItemStatus.PUBLISHED, published.status());
    }

    @Test
    void publishShouldSetCurrentPublishedRevisionId() {
        createDraftItem();
        final ContentItem published = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        assertNotNull(published.currentPublishedRevisionId());
    }

    @Test
    void publishShouldKeepWorkingRevisionIdEqualToPublishedRevisionId() {
        createDraftItem();
        final ContentItem published = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        assertEquals(published.currentPublishedRevisionId(), published.currentWorkingRevisionId());
    }

    @Test
    void publishShouldUpdateUpdatedBy() {
        createDraftItem();
        final Actor publisher = Actor.system("publisher");
        final ContentItem published = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")),
                publisher);

        assertEquals(publisher.id(), published.updatedBy());
    }

    @Test
    void publishShouldUpdateUpdatedAt() {
        createDraftItem();
        final Instant before = clock.instant();
        final Clock advancedClock = Clock.fixed(Instant.parse("2025-12-01T00:00:00Z"), ZoneOffset.UTC);
        service = new CodexContentItemService(itemRepository, revisionRepository, contentTypeRepository, versionRepository, advancedClock);

        final ContentItem published = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        assertEquals(advancedClock.instant(), published.updatedAt());
    }

    @Test
    void publishShouldMarkWorkingRevisionAsPublished() {
        final ContentItem draft = createDraftItem();
        service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        final ContentItem refreshed = service.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"), ACTOR)
                .orElseThrow();
        final ContentRevision revision = revisionRepository.findById(refreshed.currentPublishedRevisionId())
                .orElseThrow();

        assertEquals(ContentRevisionStatus.PUBLISHED, revision.status());
    }

    @Test
    void publishShouldPreserveRevisionValues() {
        createDraftItem();
        service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        final ContentItem refreshed = service.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"), ACTOR)
                .orElseThrow();
        final ContentRevision revision = revisionRepository.findById(refreshed.currentPublishedRevisionId())
                .orElseThrow();

        assertEquals("Hello World", revision.values().get(TITLE_KEY));
        assertEquals("Body text", revision.values().get(BODY_KEY));
    }

    @Test
    void publishShouldPreserveRevisionNumber() {
        createDraftItem();
        service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        final ContentItem refreshed = service.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"), ACTOR)
                .orElseThrow();
        final ContentRevision revision = revisionRepository.findById(refreshed.currentPublishedRevisionId())
                .orElseThrow();

        assertEquals(1, revision.revisionNumber());
    }

    @Test
    void publishShouldPreserveRevisionCreatedBy() {
        createDraftItem();
        service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        final ContentItem refreshed = service.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"), ACTOR)
                .orElseThrow();
        final ContentRevision revision = revisionRepository.findById(refreshed.currentPublishedRevisionId())
                .orElseThrow();

        assertEquals(ACTOR.id(), revision.createdBy());
    }

    @Test
    void publishShouldPreserveRevisionCreatedAt() {
        createDraftItem();
        final Instant expectedCreatedAt = clock.instant();
        service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        final ContentItem refreshed = service.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"), ACTOR)
                .orElseThrow();
        final ContentRevision revision = revisionRepository.findById(refreshed.currentPublishedRevisionId())
                .orElseThrow();

        assertEquals(expectedCreatedAt, revision.createdAt());
    }

    @Test
    void publishShouldReturnExistingItemWhenAlreadyPublishedAndPointersMatch() {
        createDraftItem();
        final ContentItem firstPublish = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        final ContentItem secondPublish = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        assertEquals(firstPublish, secondPublish);
    }

    @Test
    void idempotentPublishShouldNotUpdateUpdatedAt() {
        createDraftItem();
        final ContentItem firstPublish = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        final ContentItem secondPublish = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        assertEquals(firstPublish.updatedAt(), secondPublish.updatedAt());
    }

    @Test
    void idempotentPublishShouldNotUpdateUpdatedBy() {
        createDraftItem();
        final ContentItem firstPublish = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")), ACTOR);

        final Actor secondActor = Actor.system("second-publisher");
        final ContentItem secondPublish = service.publish(
                PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")),
                secondActor);

        assertEquals(firstPublish.updatedBy(), secondPublish.updatedBy());
    }

    @Test
    void publishShouldThrowNotFoundExceptionWhenItemMissing() {
        assertThrows(NotFoundException.class,
                () -> service.publish(
                        PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("ghost")),
                        ACTOR));
    }

    @Test
    void publishShouldThrowInvalidPublishExceptionWhenItemIsArchived() {
        createDraftItem();
        // force the item into ARCHIVED status by saving directly to repository
        final ContentItem draft = itemRepository.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post"))
                .orElseThrow();
        itemRepository.save(ContentItem.copyOf(draft).status(ContentItemStatus.ARCHIVED).build());

        assertThrows(InvalidContentItemPublishException.class,
                () -> service.publish(
                        PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")),
                        ACTOR));
    }

    @Test
    void publishShouldThrowInvalidPublishExceptionWhenWorkingRevisionIsArchived() {
        final ContentItem draft = createDraftItem();
        // force the working revision into ARCHIVED status
        final ContentRevision working = revisionRepository.findById(draft.currentWorkingRevisionId())
                .orElseThrow();
        revisionRepository.save(ContentRevision.copyOf(working).status(ContentRevisionStatus.ARCHIVED).build());

        assertThrows(InvalidContentItemPublishException.class,
                () -> service.publish(
                        PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")),
                        ACTOR));
    }

    @Test
    void publishRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.publish(null, ACTOR));
    }

    @Test
    void publishRejectsNullActor() {
        createDraftItem();
        assertThrows(NullPointerException.class,
                () -> service.publish(
                        PublishContentItemCommand.of(SITE_ACME, BLOG_POST, ContentItemKey.of("my-post")),
                        null));
    }
}
