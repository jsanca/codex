package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoryContentTypeRepositoryTest {

    private MemoryContentTypeRepository repository;

    private final ContentTypeKey key = ContentTypeKey.of("blog-post");
    private final ContentTypeId id = ContentTypeId.generate();

    @BeforeEach
    void setUp() {
        repository = new MemoryContentTypeRepository();
    }

    @Test
    @DisplayName("save should return saved content type")
    void saveShouldReturnSavedContentType() {
        final ContentType contentType = buildContentType();
        final ContentType result = repository.save(contentType);
        assertEquals(contentType, result);
    }

    @Test
    @DisplayName("findByKey should return saved content type")
    void findByKeyShouldReturnSavedContentType() {
        final ContentType contentType = buildContentType();
        repository.save(contentType);
        final Optional<ContentType> result = repository.findByKey(key);
        assertTrue(result.isPresent());
        assertEquals(contentType, result.get());
    }

    @Test
    @DisplayName("findByKey should return empty when missing")
    void findByKeyShouldReturnEmptyWhenMissing() {
        assertTrue(repository.findByKey(key).isEmpty());
    }

    @Test
    @DisplayName("existsByKey should return true when saved")
    void existsByKeyShouldReturnTrueWhenSaved() {
        repository.save(buildContentType());
        assertTrue(repository.existsByKey(key));
    }

    @Test
    @DisplayName("existsByKey should return false when missing")
    void existsByKeyShouldReturnFalseWhenMissing() {
        assertFalse(repository.existsByKey(key));
    }

    @Test
    @DisplayName("findAll should return all saved content types")
    void findAllShouldReturnAllSavedContentTypes() {
        repository.save(buildContentType());
        repository.save(ContentType.builder()
                .id(ContentTypeId.generate())
                .key(ContentTypeKey.of("product"))
                .displayName("Product")
                .build());
        final List<ContentType> all = repository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("required arguments should not accept null")
    void requiredArgumentsShouldNotAcceptNull() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.findByKey(null));
        assertThrows(NullPointerException.class, () -> repository.existsByKey(null));
    }

    private ContentType buildContentType() {
        return ContentType.builder()
                .id(id)
                .key(key)
                .displayName("Blog Post")
                .build();
    }
}
