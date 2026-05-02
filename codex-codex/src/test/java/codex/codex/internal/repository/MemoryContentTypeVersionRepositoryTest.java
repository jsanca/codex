package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentTypeVersion;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentTypeVersionStatus;
import codex.codex.api.model.value.FieldType;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoryContentTypeVersionRepositoryTest {

    private MemoryContentTypeVersionRepository repository;

    private final ContentTypeId contentTypeId = ContentTypeId.generate();
    private final SiteKey siteKey = SiteKey.of("acme");
    private final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
    private final ActorId actorId = ActorId.of("system:test");

    @BeforeEach
    void setUp() {
        repository = new MemoryContentTypeVersionRepository();
    }

    @Test
    @DisplayName("save should return saved version")
    void saveShouldReturnSavedVersion() {
        final ContentTypeVersion v = build(1, ContentTypeVersionStatus.PUBLISHED);
        assertEquals(v, repository.save(v));
    }

    @Test
    @DisplayName("findById should return saved version")
    void findByIdShouldReturnSavedVersion() {
        final ContentTypeVersion v = build(1, ContentTypeVersionStatus.PUBLISHED);
        repository.save(v);
        final Optional<ContentTypeVersion> result = repository.findById(v.id());
        assertTrue(result.isPresent());
        assertEquals(v, result.get());
    }

    @Test
    @DisplayName("findById should return empty when missing")
    void findByIdShouldReturnEmptyWhenMissing() {
        assertTrue(repository.findById(ContentTypeVersionId.generate()).isEmpty());
    }

    @Test
    @DisplayName("findByContentTypeAndVersion should return saved version")
    void findByContentTypeAndVersionShouldReturnSaved() {
        final ContentTypeVersion v = build(1, ContentTypeVersionStatus.PUBLISHED);
        repository.save(v);
        final Optional<ContentTypeVersion> result = repository.findByContentTypeAndVersion(contentTypeId, 1);
        assertTrue(result.isPresent());
        assertEquals(v, result.get());
    }

    @Test
    @DisplayName("findByContentTypeAndVersion should return empty when missing")
    void findByContentTypeAndVersionShouldReturnEmptyWhenMissing() {
        assertTrue(repository.findByContentTypeAndVersion(contentTypeId, 1).isEmpty());
    }

    @Test
    @DisplayName("findByContentType should return all versions for that content type")
    void findByContentTypeShouldReturnAllVersions() {
        repository.save(build(1, ContentTypeVersionStatus.PUBLISHED));
        repository.save(build(2, ContentTypeVersionStatus.DEPRECATED));
        final List<ContentTypeVersion> result = repository.findByContentType(contentTypeId);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findByContentType should not return versions for other content types")
    void findByContentTypeShouldNotReturnOtherContentTypes() {
        repository.save(build(1, ContentTypeVersionStatus.PUBLISHED));
        final ContentTypeVersion other = ContentTypeVersion.builder()
                .id(ContentTypeVersionId.generate())
                .contentTypeId(ContentTypeId.generate())
                .siteKey(siteKey)
                .contentTypeKey(contentTypeKey)
                .version(1)
                .createdBy(actorId)
                .build();
        repository.save(other);
        final List<ContentTypeVersion> result = repository.findByContentType(contentTypeId);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findLatestPublished should return highest published version")
    void findLatestPublishedShouldReturnHighestPublishedVersion() {
        repository.save(build(1, ContentTypeVersionStatus.PUBLISHED));
        repository.save(build(2, ContentTypeVersionStatus.PUBLISHED));
        final Optional<ContentTypeVersion> result = repository.findLatestPublished(contentTypeId);
        assertTrue(result.isPresent());
        assertEquals(2, result.get().version());
    }

    @Test
    @DisplayName("findLatestPublished should ignore deprecated and archived versions")
    void findLatestPublishedShouldIgnoreDeprecatedAndArchived() {
        repository.save(build(1, ContentTypeVersionStatus.PUBLISHED));
        repository.save(build(2, ContentTypeVersionStatus.DEPRECATED));
        repository.save(build(3, ContentTypeVersionStatus.ARCHIVED));
        final Optional<ContentTypeVersion> result = repository.findLatestPublished(contentTypeId);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().version());
    }

    @Test
    @DisplayName("findLatestPublished should return empty when no published versions exist")
    void findLatestPublishedShouldReturnEmptyWhenNonePublished() {
        repository.save(build(1, ContentTypeVersionStatus.DEPRECATED));
        assertTrue(repository.findLatestPublished(contentTypeId).isEmpty());
    }

    @Test
    @DisplayName("required arguments should reject null")
    void requiredArgumentsShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.findById(null));
        assertThrows(NullPointerException.class, () -> repository.findByContentTypeAndVersion(null, 1));
        assertThrows(NullPointerException.class, () -> repository.findLatestPublished(null));
        assertThrows(NullPointerException.class, () -> repository.findByContentType(null));
    }

    @Test
    @DisplayName("findByContentTypeAndVersion should reject version less than 1")
    void findByContentTypeAndVersionShouldRejectVersionLessThan1() {
        assertThrows(IllegalArgumentException.class, () ->
                repository.findByContentTypeAndVersion(contentTypeId, 0));
    }

    // --- snapshot immutability ---

    @Test
    @DisplayName("version fields are immutable snapshot — mutating original map does not affect saved version")
    void versionFieldsAreImmutableSnapshot() {
        final FieldKey titleKey = FieldKey.of("title");
        final java.util.Map<FieldKey, codex.codex.api.model.entity.Field> fields = new java.util.HashMap<>();
        fields.put(titleKey, codex.codex.api.model.entity.Field.builder().key(titleKey).type(FieldType.TEXT).build());

        final ContentTypeVersion v = ContentTypeVersion.builder()
                .id(ContentTypeVersionId.generate())
                .contentTypeId(contentTypeId)
                .siteKey(siteKey)
                .contentTypeKey(contentTypeKey)
                .version(1)
                .fields(fields)
                .createdBy(actorId)
                .build();

        repository.save(v);
        fields.put(FieldKey.of("body"),
                codex.codex.api.model.entity.Field.builder().key("body").type(FieldType.TEXT).build());

        final ContentTypeVersion found = repository.findById(v.id()).orElseThrow();
        assertEquals(1, found.fields().size());
        assertTrue(found.fields().containsKey(titleKey));
    }

    // --- helpers ---

    private ContentTypeVersion build(final int version, final ContentTypeVersionStatus status) {
        final ContentTypeVersionId vId = ContentTypeVersionId.forVersion(
                siteKey.value(), contentTypeKey.value(), version);
        return ContentTypeVersion.builder()
                .id(vId)
                .contentTypeId(contentTypeId)
                .siteKey(siteKey)
                .contentTypeKey(contentTypeKey)
                .version(version)
                .status(status)
                .createdBy(actorId)
                .build();
    }
}
