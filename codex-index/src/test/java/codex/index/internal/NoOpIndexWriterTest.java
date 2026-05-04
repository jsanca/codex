package codex.index.internal;

import codex.codex.api.model.identity.SiteKey;
import codex.index.api.IndexDocument;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexResourceType;
import codex.index.api.IndexWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NoOpIndexWriter}.
 */
class NoOpIndexWriterTest {

    private static final IndexDocumentId ID = IndexDocumentId.of("content-item:acme:blog-post:my-post");
    private static final IndexWriter writer = new NoOpIndexWriter();

    private IndexDocument minimalDocument() {
        return IndexDocument.builder()
                .id(ID)
                .resourceType(IndexResourceType.CONTENT_ITEM)
                .siteKey(SiteKey.of("acme"))
                .build();
    }

    @Test
    void upsertRejectsNullDocument() {
        assertThrows(NullPointerException.class, () -> writer.upsert(null));
    }

    @Test
    void deleteRejectsNullId() {
        assertThrows(NullPointerException.class, () -> writer.delete(null));
    }

    @Test
    void upsertSilentlyDiscards() {
        assertDoesNotThrow(() -> writer.upsert(minimalDocument()));
    }

    @Test
    void deleteSilentlyDiscards() {
        assertDoesNotThrow(() -> writer.delete(ID));
    }
}
