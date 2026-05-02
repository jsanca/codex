package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.FieldType;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoryContentTypeRepositoryTest {

    private MemoryContentTypeRepository repository;

    private final SiteKey siteKey = SiteKey.of("acme");
    private final ContentTypeKey key = ContentTypeKey.of("blog-post");
    private final ActorId actorId = ActorId.of("system:test");

    @BeforeEach
    void setUp() {
        repository = new MemoryContentTypeRepository();
    }

    @Test
    @DisplayName("save should return saved content type")
    void saveShouldReturnSavedContentType() {
        final ContentType ct = build(siteKey, key);
        assertEquals(ct, repository.save(ct));
    }

    @Test
    @DisplayName("findByKey should return saved content type using siteKey + key")
    void findByKeyShouldReturnSavedContentType() {
        repository.save(build(siteKey, key));
        final Optional<ContentType> result = repository.findByKey(siteKey, key);
        assertTrue(result.isPresent());
        assertEquals(key, result.get().key());
        assertEquals(siteKey, result.get().siteKey());
    }

    @Test
    @DisplayName("findByKey should return empty when siteKey differs")
    void findByKeyShouldReturnEmptyWhenSiteKeyDiffers() {
        repository.save(build(siteKey, key));
        assertTrue(repository.findByKey(SiteKey.of("other-site"), key).isEmpty());
    }

    @Test
    @DisplayName("findByKey should return empty when missing")
    void findByKeyShouldReturnEmptyWhenMissing() {
        assertTrue(repository.findByKey(siteKey, key).isEmpty());
    }

    @Test
    @DisplayName("existsByKey should return true when saved with same siteKey + key")
    void existsByKeyShouldReturnTrueWhenSaved() {
        repository.save(build(siteKey, key));
        assertTrue(repository.existsByKey(siteKey, key));
    }

    @Test
    @DisplayName("existsByKey should return false when siteKey differs")
    void existsByKeyShouldReturnFalseWhenSiteKeyDiffers() {
        repository.save(build(siteKey, key));
        assertFalse(repository.existsByKey(SiteKey.of("other-site"), key));
    }

    @Test
    @DisplayName("existsByKey should return false when missing")
    void existsByKeyShouldReturnFalseWhenMissing() {
        assertFalse(repository.existsByKey(siteKey, key));
    }

    @Test
    @DisplayName("same ContentTypeKey can exist under different SiteKey values")
    void sameKeyCanExistUnderDifferentSites() {
        final SiteKey siteA = SiteKey.of("site-a");
        final SiteKey siteB = SiteKey.of("site-b");
        repository.save(build(siteA, key));
        repository.save(build(siteB, key));
        assertTrue(repository.existsByKey(siteA, key));
        assertTrue(repository.existsByKey(siteB, key));
        assertEquals(2, repository.findAll().size());
    }

    @Test
    @DisplayName("findBySiteKey returns only content types for that site")
    void findBySiteKeyReturnsOnlyMatchingSite() {
        repository.save(build(siteKey, key));
        repository.save(build(siteKey, ContentTypeKey.of("product")));
        repository.save(build(SiteKey.of("other-site"), key));
        final List<ContentType> result = repository.findBySiteKey(siteKey);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(ct -> ct.siteKey().equals(siteKey)));
    }

    @Test
    @DisplayName("findBySiteKey returns empty list when no content types exist for that site")
    void findBySiteKeyReturnsEmptyWhenNoneForSite() {
        assertTrue(repository.findBySiteKey(siteKey).isEmpty());
    }

    @Test
    @DisplayName("findAll should return all saved content types")
    void findAllShouldReturnAllSavedContentTypes() {
        repository.save(build(siteKey, key));
        repository.save(build(SiteKey.SYSTEM, ContentTypeKey.of("global-type")));
        assertEquals(2, repository.findAll().size());
    }

    @Test
    @DisplayName("saving a content type with fields preserves fields")
    void savingContentTypeWithFieldsPreservesFields() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field titleField = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final ContentType ct = ContentType.builder()
                .id(ContentTypeId.generate())
                .siteKey(siteKey)
                .key(key)
                .displayName("Blog Post")
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .fields(java.util.Map.of(titleKey, titleField))
                .build();
        final ContentType saved = repository.save(ct);
        assertEquals(1, saved.fields().size());
        assertEquals(titleField, saved.fields().get(titleKey));
    }

    @Test
    @DisplayName("finding a content type with fields returns fields")
    void findingContentTypeWithFieldsReturnsFields() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field titleField = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final ContentType ct = ContentType.builder()
                .id(ContentTypeId.generate())
                .siteKey(siteKey)
                .key(key)
                .displayName("Blog Post")
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .fields(java.util.Map.of(titleKey, titleField))
                .build();
        repository.save(ct);
        final ContentType found = repository.findByKey(siteKey, key).orElseThrow();
        assertEquals(1, found.fields().size());
        assertEquals(titleField, found.fields().get(titleKey));
    }

    @Test
    @DisplayName("required arguments should not accept null")
    void requiredArgumentsShouldNotAcceptNull() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.findByKey(null, key));
        assertThrows(NullPointerException.class, () -> repository.findByKey(siteKey, null));
        assertThrows(NullPointerException.class, () -> repository.existsByKey(null, key));
        assertThrows(NullPointerException.class, () -> repository.existsByKey(siteKey, null));
        assertThrows(NullPointerException.class, () -> repository.findBySiteKey(null));
    }

    private ContentType build(final SiteKey sk, final ContentTypeKey ctk) {
        return ContentType.builder()
                .id(ContentTypeId.generate())
                .siteKey(sk)
                .key(ctk)
                .displayName("Test Type")
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();
    }
}
