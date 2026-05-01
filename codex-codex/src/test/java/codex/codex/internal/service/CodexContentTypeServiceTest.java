package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.value.ContentTypeStatus;
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
    private final ContentTypeKey key = ContentTypeKey.of("blog-post");
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        repository = new MemoryContentTypeRepository();
        service = new CodexContentTypeService(repository, clock);
    }

    @Test
    @DisplayName("create should create draft content type")
    void createShouldCreateDraftContentType() {
        final ContentType result = service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        assertNotNull(result.id());
        assertEquals(key, result.key());
        assertEquals("Blog Post", result.displayName());
        assertEquals(ContentTypeStatus.DRAFT, result.status());
        assertEquals(clock.instant(), result.createdAt());
    }

    @Test
    @DisplayName("create should throw when key already exists")
    void createShouldThrowWhenKeyAlreadyExists() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        assertThrows(ContentTypeAlreadyExistsException.class, () ->
                service.create(CreateContentTypeCommand.of(key, "Another"), actor));
    }

    @Test
    @DisplayName("findByKey should return existing content type")
    void findByKeyShouldReturnExistingContentType() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        final Optional<ContentType> result = service.findByKey(key, actor);
        assertTrue(result.isPresent());
        assertEquals(key, result.get().key());
    }

    @Test
    @DisplayName("findByKey should return empty when missing")
    void findByKeyShouldReturnEmptyWhenMissing() {
        assertTrue(service.findByKey(key, actor).isEmpty());
    }

    @Test
    @DisplayName("findAll should return all content types")
    void findAllShouldReturnAllContentTypes() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        service.create(CreateContentTypeCommand.of(ContentTypeKey.of("product"), "Product"), actor);
        final List<ContentType> all = service.findAll(actor);
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("activate should move draft to active")
    void activateShouldMoveDraftToActive() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        final ContentType result = service.activate(ActivateContentTypeCommand.of(key), actor);
        assertEquals(ContentTypeStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate should be idempotent when already active")
    void activateShouldBeIdempotentWhenAlreadyActive() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        service.activate(ActivateContentTypeCommand.of(key), actor);
        final ContentType result = service.activate(ActivateContentTypeCommand.of(key), actor);
        assertEquals(ContentTypeStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate should fail when archived")
    void activateShouldFailWhenArchived() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        service.archive(ArchiveContentTypeCommand.of(key), actor);
        assertThrows(InvalidContentTypeStatusTransitionException.class, () ->
                service.activate(ActivateContentTypeCommand.of(key), actor));
    }

    @Test
    @DisplayName("archive should move draft to archived")
    void archiveShouldMoveDraftToArchived() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        final ContentType result = service.archive(ArchiveContentTypeCommand.of(key), actor);
        assertEquals(ContentTypeStatus.ARCHIVED, result.status());
    }

    @Test
    @DisplayName("archive should move active to archived")
    void archiveShouldMoveActiveToArchived() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        service.activate(ActivateContentTypeCommand.of(key), actor);
        final ContentType result = service.archive(ArchiveContentTypeCommand.of(key), actor);
        assertEquals(ContentTypeStatus.ARCHIVED, result.status());
    }

    @Test
    @DisplayName("archive should be idempotent when already archived")
    void archiveShouldBeIdempotentWhenAlreadyArchived() {
        service.create(CreateContentTypeCommand.of(key, "Blog Post"), actor);
        service.archive(ArchiveContentTypeCommand.of(key), actor);
        final ContentType result = service.archive(ArchiveContentTypeCommand.of(key), actor);
        assertEquals(ContentTypeStatus.ARCHIVED, result.status());
    }

    @Test
    @DisplayName("missing content type should throw NotFoundException for activate")
    void missingContentTypeShouldThrowNotFoundExceptionForActivate() {
        assertThrows(NotFoundException.class, () ->
                service.activate(ActivateContentTypeCommand.of(key), actor));
    }

    @Test
    @DisplayName("missing content type should throw NotFoundException for archive")
    void missingContentTypeShouldThrowNotFoundExceptionForArchive() {
        assertThrows(NotFoundException.class, () ->
                service.archive(ArchiveContentTypeCommand.of(key), actor));
    }

    @Test
    @DisplayName("required arguments should not accept null")
    void requiredArgumentsShouldNotAcceptNull() {
        assertThrows(NullPointerException.class, () -> service.create(null, actor));
        assertThrows(NullPointerException.class, () ->
                service.create(CreateContentTypeCommand.of(key, "Blog Post"), null));
        assertThrows(NullPointerException.class, () -> service.activate(null, actor));
        assertThrows(NullPointerException.class, () ->
                service.activate(ActivateContentTypeCommand.of(key), null));
        assertThrows(NullPointerException.class, () -> service.archive(null, actor));
        assertThrows(NullPointerException.class, () ->
                service.archive(ArchiveContentTypeCommand.of(key), null));
        assertThrows(NullPointerException.class, () -> service.findByKey(null, actor));
        assertThrows(NullPointerException.class, () -> service.findByKey(key, null));
        assertThrows(NullPointerException.class, () -> service.findAll(null));
    }
}
