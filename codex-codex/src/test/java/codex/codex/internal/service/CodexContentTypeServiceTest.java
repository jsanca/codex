package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.codex.api.model.value.FieldType;
import codex.codex.internal.repository.MemoryContentTypeRepository;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CodexContentTypeServiceTest {

    private MemoryContentTypeRepository repository;
    private CodexContentTypeService service;

    private final Actor actor = Actor.system("test");
    private final SiteKey siteKey = SiteKey.of("acme");
    private final ContentTypeKey key = ContentTypeKey.of("blog-post");
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        repository = new MemoryContentTypeRepository();
        service = new CodexContentTypeService(repository, clock);
    }

    // --- create ---

    @Test
    @DisplayName("create should create draft content type with siteKey")
    void createShouldCreateDraftContentTypeWithSiteKey() {
        final ContentType result = service.create(cmd("Blog Post"), actor);
        assertNotNull(result.id());
        assertEquals(siteKey, result.siteKey());
        assertEquals(key, result.key());
        assertEquals("Blog Post", result.displayName());
        assertEquals(ContentTypeStatus.DRAFT, result.status());
        assertEquals(clock.instant(), result.createdAt());
        assertEquals(clock.instant(), result.updatedAt());
    }

    @Test
    @DisplayName("create should set owner from actor id")
    void createShouldSetOwnerFromActorId() {
        final ContentType result = service.create(cmd("Blog Post"), actor);
        assertEquals(actor.id(), result.owner());
    }

    @Test
    @DisplayName("create should set createdBy from actor id")
    void createShouldSetCreatedByFromActorId() {
        final ContentType result = service.create(cmd("Blog Post"), actor);
        assertEquals(actor.id(), result.createdBy());
    }

    @Test
    @DisplayName("create should set updatedBy from actor id")
    void createShouldSetUpdatedByFromActorId() {
        final ContentType result = service.create(cmd("Blog Post"), actor);
        assertEquals(actor.id(), result.updatedBy());
    }

    @Test
    @DisplayName("create should allow same content type key under different site keys")
    void createShouldAllowSameKeyUnderDifferentSites() {
        service.create(cmd("Blog Post"), actor);
        final SiteKey other = SiteKey.of("other-site");
        final ContentType result = service.create(
                CreateContentTypeCommand.of(other, key, "Blog Post"), actor);
        assertEquals(other, result.siteKey());
    }

    @Test
    @DisplayName("create should reject duplicate key in same site")
    void createShouldRejectDuplicateKeyInSameSite() {
        service.create(cmd("Blog Post"), actor);
        assertThrows(ContentTypeAlreadyExistsException.class, () ->
                service.create(cmd("Another"), actor));
    }

    @Test
    @DisplayName("create should allow global content type using SiteKey.SYSTEM")
    void createShouldAllowGlobalContentTypeUsingSiteKeySystem() {
        final ContentType result = service.create(
                CreateContentTypeCommand.of(SiteKey.SYSTEM, key, "Global Blog Post"), actor);
        assertEquals(SiteKey.SYSTEM, result.siteKey());
        assertEquals(key, result.key());
    }

    // --- findByKey ---

    @Test
    @DisplayName("findByKey should find by siteKey + key")
    void findByKeyShouldFindBySiteKeyAndKey() {
        service.create(cmd("Blog Post"), actor);
        final Optional<ContentType> result = service.findByKey(siteKey, key, actor);
        assertTrue(result.isPresent());
        assertEquals(key, result.get().key());
    }

    @Test
    @DisplayName("findByKey should return empty when siteKey differs")
    void findByKeyShouldReturnEmptyWhenSiteKeyDiffers() {
        service.create(cmd("Blog Post"), actor);
        assertTrue(service.findByKey(SiteKey.of("other-site"), key, actor).isEmpty());
    }

    @Test
    @DisplayName("findByKey should return empty when missing")
    void findByKeyShouldReturnEmptyWhenMissing() {
        assertTrue(service.findByKey(siteKey, key, actor).isEmpty());
    }

    @Test
    @DisplayName("findAll should return all content types")
    void findAllShouldReturnAllContentTypes() {
        service.create(cmd("Blog Post"), actor);
        service.create(CreateContentTypeCommand.of(SiteKey.SYSTEM, ContentTypeKey.of("product"), "Product"), actor);
        final List<ContentType> all = service.findAll(actor);
        assertEquals(2, all.size());
    }

    // --- activate ---

    @Test
    @DisplayName("activate should move draft to active")
    void activateShouldMoveDraftToActive() {
        service.create(cmd("Blog Post"), actor);
        final ContentType result = service.activate(activateCmd(), actor);
        assertEquals(ContentTypeStatus.ACTIVE, result.status());

        final ContentType first = service.activate(activateCmd(), actor);
        final Actor other = Actor.system("other");

        final ContentType second = service.activate(activateCmd(), other);

        assertSame(first, second);
        assertEquals(first.updatedBy(), second.updatedBy());
        assertEquals(first.updatedAt(), second.updatedAt());
    }

    @Test
    @DisplayName("activate should set updatedBy from actor id")
    void activateShouldSetUpdatedByFromActorId() {
        service.create(cmd("Blog Post"), actor);
        final Actor other = Actor.system("other");
        final ContentType result = service.activate(activateCmd(), other);
        assertEquals(other.id(), result.updatedBy());
    }

    @Test
    @DisplayName("activate should update updatedAt")
    void activateShouldUpdateUpdatedAt() {
        service.create(cmd("Blog Post"), actor);
        final ContentType result = service.activate(activateCmd(), actor);
        assertEquals(clock.instant(), result.updatedAt());
    }

    @Test
    @DisplayName("activate should be idempotent when already active")
    void activateShouldBeIdempotentWhenAlreadyActive() {
        service.create(cmd("Blog Post"), actor);
        service.activate(activateCmd(), actor);
        final ContentType result = service.activate(activateCmd(), actor);
        assertEquals(ContentTypeStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate should fail when archived")
    void activateShouldFailWhenArchived() {
        service.create(cmd("Blog Post"), actor);
        service.archive(archiveCmd(), actor);
        assertThrows(InvalidContentTypeStatusTransitionException.class, () ->
                service.activate(activateCmd(), actor));
    }

    @Test
    @DisplayName("missing content type should throw NotFoundException for activate")
    void missingContentTypeShouldThrowNotFoundExceptionForActivate() {
        assertThrows(NotFoundException.class, () -> service.activate(activateCmd(), actor));
    }

    // --- archive ---

    @Test
    @DisplayName("archive should move draft to archived and set updatedBy from actor id")
    void archiveShouldMoveDraftToArchivedAndSetUpdatedBy() {
        service.create(cmd("Blog Post"), actor);
        final ContentType result = service.archive(archiveCmd(), actor);
        assertEquals(ContentTypeStatus.ARCHIVED, result.status());
        assertEquals(actor.id(), result.updatedBy());
    }

    @Test
    @DisplayName("archive should move active to archived and set updatedBy from actor id")
    void archiveShouldMoveActiveToArchivedAndSetUpdatedBy() {
        service.create(cmd("Blog Post"), actor);
        service.activate(activateCmd(), actor);
        final ContentType result = service.archive(archiveCmd(), actor);
        assertEquals(ContentTypeStatus.ARCHIVED, result.status());
        assertEquals(actor.id(), result.updatedBy());
    }

    @Test
    @DisplayName("archive should be idempotent when already archived")
    void archiveShouldBeIdempotentWhenAlreadyArchived() {
        service.create(cmd("Blog Post"), actor);
        service.archive(archiveCmd(), actor);
        final ContentType result = service.archive(archiveCmd(), actor);
        assertEquals(ContentTypeStatus.ARCHIVED, result.status());
    }

    @Test
    @DisplayName("missing content type should throw NotFoundException for archive")
    void missingContentTypeShouldThrowNotFoundExceptionForArchive() {
        assertThrows(NotFoundException.class, () -> service.archive(archiveCmd(), actor));
    }

    // --- addField ---

    @Test
    @DisplayName("addField should add field to draft content type")
    void addFieldShouldAddFieldToDraft() {
        service.create(cmd("Blog Post"), actor);
        final Field field = titleField();
        final ContentType result = service.addField(addFieldCmd(field), actor);
        assertEquals(1, result.fields().size());
        assertEquals(field, result.fields().get(FieldKey.of("title")));
    }

    @Test
    @DisplayName("addField should update updatedBy from actor id")
    void addFieldShouldUpdateUpdatedBy() {
        service.create(cmd("Blog Post"), actor);
        final Actor other = Actor.system("editor");
        final ContentType result = service.addField(addFieldCmd(titleField()), other);
        assertEquals(other.id(), result.updatedBy());
    }

    @Test
    @DisplayName("addField should update updatedAt")
    void addFieldShouldUpdateUpdatedAt() {
        service.create(cmd("Blog Post"), actor);
        final ContentType result = service.addField(addFieldCmd(titleField()), actor);
        assertEquals(clock.instant(), result.updatedAt());
    }

    @Test
    @DisplayName("addField should reject duplicate field key")
    void addFieldShouldRejectDuplicateFieldKey() {
        service.create(cmd("Blog Post"), actor);
        service.addField(addFieldCmd(titleField()), actor);
        assertThrows(ContentTypeFieldAlreadyExistsException.class, () ->
                service.addField(addFieldCmd(titleField()), actor));
    }

    @Test
    @DisplayName("addField should throw NotFoundException when content type is missing")
    void addFieldShouldThrowNotFoundWhenMissing() {
        assertThrows(codex.fundamentum.api.exception.NotFoundException.class, () ->
                service.addField(addFieldCmd(titleField()), actor));
    }

    @Test
    @DisplayName("addField should reject ACTIVE content type")
    void addFieldShouldRejectActiveContentType() {
        service.create(cmd("Blog Post"), actor);
        service.activate(activateCmd(), actor);
        assertThrows(InvalidContentTypeSchemaModificationException.class, () ->
                service.addField(addFieldCmd(titleField()), actor));
    }

    @Test
    @DisplayName("addField should reject ARCHIVED content type")
    void addFieldShouldRejectArchivedContentType() {
        service.create(cmd("Blog Post"), actor);
        service.archive(archiveCmd(), actor);
        assertThrows(InvalidContentTypeSchemaModificationException.class, () ->
                service.addField(addFieldCmd(titleField()), actor));
    }

    @Test
    @DisplayName("addField should reject null arguments")
    void addFieldShouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> service.addField(null, actor));
        assertThrows(NullPointerException.class, () -> service.addField(addFieldCmd(titleField()), null));
    }

    // --- removeField ---

    @Test
    @DisplayName("removeField should remove field from draft content type")
    void removeFieldShouldRemoveFieldFromDraft() {
        service.create(cmd("Blog Post"), actor);
        service.addField(addFieldCmd(titleField()), actor);
        final ContentType result = service.removeField(removeFieldCmd(FieldKey.of("title")), actor);
        assertTrue(result.fields().isEmpty());
    }

    @Test
    @DisplayName("removeField should update updatedBy from actor id")
    void removeFieldShouldUpdateUpdatedBy() {
        service.create(cmd("Blog Post"), actor);
        service.addField(addFieldCmd(titleField()), actor);
        final Actor other = Actor.system("editor");
        final ContentType result = service.removeField(removeFieldCmd(FieldKey.of("title")), other);
        assertEquals(other.id(), result.updatedBy());
    }

    @Test
    @DisplayName("removeField should update updatedAt")
    void removeFieldShouldUpdateUpdatedAt() {
        service.create(cmd("Blog Post"), actor);
        service.addField(addFieldCmd(titleField()), actor);
        final ContentType result = service.removeField(removeFieldCmd(FieldKey.of("title")), actor);
        assertEquals(clock.instant(), result.updatedAt());
    }

    @Test
    @DisplayName("removeField should throw ContentTypeFieldNotFoundException for missing field key")
    void removeFieldShouldThrowNotFoundForMissingField() {
        service.create(cmd("Blog Post"), actor);
        assertThrows(ContentTypeFieldNotFoundException.class, () ->
                service.removeField(removeFieldCmd(FieldKey.of("title")), actor));
    }

    @Test
    @DisplayName("removeField should throw NotFoundException when content type is missing")
    void removeFieldShouldThrowNotFoundWhenContentTypeMissing() {
        assertThrows(codex.fundamentum.api.exception.NotFoundException.class, () ->
                service.removeField(removeFieldCmd(FieldKey.of("title")), actor));
    }

    @Test
    @DisplayName("removeField should reject ACTIVE content type")
    void removeFieldShouldRejectActiveContentType() {
        service.create(cmd("Blog Post"), actor);
        service.activate(activateCmd(), actor);
        assertThrows(InvalidContentTypeSchemaModificationException.class, () ->
                service.removeField(removeFieldCmd(FieldKey.of("title")), actor));
    }

    @Test
    @DisplayName("removeField should reject ARCHIVED content type")
    void removeFieldShouldRejectArchivedContentType() {
        service.create(cmd("Blog Post"), actor);
        service.archive(archiveCmd(), actor);
        assertThrows(InvalidContentTypeSchemaModificationException.class, () ->
                service.removeField(removeFieldCmd(FieldKey.of("title")), actor));
    }

    @Test
    @DisplayName("removeField should reject null arguments")
    void removeFieldShouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> service.removeField(null, actor));
        assertThrows(NullPointerException.class, () ->
                service.removeField(removeFieldCmd(FieldKey.of("title")), null));
    }

    // --- null checks ---

    @Test
    @DisplayName("required arguments should not accept null")
    void requiredArgumentsShouldNotAcceptNull() {
        assertThrows(NullPointerException.class, () -> service.create(null, actor));
        assertThrows(NullPointerException.class, () -> service.create(cmd("Blog Post"), null));
        assertThrows(NullPointerException.class, () -> service.activate(null, actor));
        assertThrows(NullPointerException.class, () -> service.activate(activateCmd(), null));
        assertThrows(NullPointerException.class, () -> service.archive(null, actor));
        assertThrows(NullPointerException.class, () -> service.archive(archiveCmd(), null));
        assertThrows(NullPointerException.class, () -> service.findByKey(null, key, actor));
        assertThrows(NullPointerException.class, () -> service.findByKey(siteKey, null, actor));
        assertThrows(NullPointerException.class, () -> service.findByKey(siteKey, key, null));
        assertThrows(NullPointerException.class, () -> service.findAll(null));
        assertThrows(NullPointerException.class, () -> service.findBySiteKey(null, actor));
        assertThrows(NullPointerException.class, () -> service.findBySiteKey(siteKey, null));
    }

    // --- helpers ---

    private CreateContentTypeCommand cmd(final String displayName) {
        return CreateContentTypeCommand.of(siteKey, key, displayName);
    }

    private ActivateContentTypeCommand activateCmd() {
        return ActivateContentTypeCommand.of(siteKey, key);
    }

    private ArchiveContentTypeCommand archiveCmd() {
        return ArchiveContentTypeCommand.of(siteKey, key);
    }

    private Field titleField() {
        return Field.builder().key("title").type(FieldType.TEXT).displayName("Title").build();
    }

    private AddContentTypeFieldCommand addFieldCmd(final Field field) {
        return AddContentTypeFieldCommand.of(siteKey, key, field);
    }

    private RemoveContentTypeFieldCommand removeFieldCmd(final FieldKey fieldKey) {
        return RemoveContentTypeFieldCommand.of(siteKey, key, fieldKey);
    }
}
