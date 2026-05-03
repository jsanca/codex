package codex.codex.internal.index;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexDocumentId;
import codex.codex.api.index.IndexResourceType;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests for {@link NoOpIndexWriter}.
 */
class NoOpIndexWriterTest {

    private static final NoOpIndexWriter WRITER = new NoOpIndexWriter();

    private static IndexDocument minimalDocument() {
        return IndexDocument.builder()
                .id(IndexDocumentId.of("site:acme"))
                .resourceType(IndexResourceType.SITE)
                .siteKey(SiteKey.of("acme"))
                .build();
    }

    @Test
    void upsertAcceptsValidDocument() {
        assertDoesNotThrow(() -> WRITER.upsert(minimalDocument()));
    }

    @Test
    void deleteAcceptsValidId() {
        assertDoesNotThrow(() -> WRITER.delete(IndexDocumentId.of("site:acme")));
    }

    @Test
    void upsertRejectsNullDocument() {
        assertThrows(NullPointerException.class, () -> WRITER.upsert(null));
    }

    @Test
    void deleteRejectsNullId() {
        assertThrows(NullPointerException.class, () -> WRITER.delete(null));
    }
}
