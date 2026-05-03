package codex.codex.internal.index;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexDocumentId;
import codex.codex.api.index.IndexResourceType;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RecordingIndexWriter}.
 */
class RecordingIndexWriterTest {

    private RecordingIndexWriter writer;

    @BeforeEach
    void setUp() {
        writer = new RecordingIndexWriter();
    }

    private static IndexDocument documentFor(final String id) {
        return IndexDocument.builder()
                .id(IndexDocumentId.of(id))
                .resourceType(IndexResourceType.SITE)
                .siteKey(SiteKey.of("acme"))
                .build();
    }

    // --- upsert ---

    @Test
    void recordsUpsertedDocument() {
        final IndexDocument doc = documentFor("site:acme");
        writer.upsert(doc);
        assertEquals(List.of(doc), writer.upserts());
    }

    @Test
    void upsertRejectsNullDocument() {
        assertThrows(NullPointerException.class, () -> writer.upsert(null));
    }

    // --- delete ---

    @Test
    void recordsDeletedId() {
        final IndexDocumentId id = IndexDocumentId.of("site:acme");
        writer.delete(id);
        assertEquals(List.of(id), writer.deletes());
    }

    @Test
    void deleteRejectsNullId() {
        assertThrows(NullPointerException.class, () -> writer.delete(null));
    }

    // --- immutability of snapshots ---

    @Test
    void upsertsSnapshotIsImmutable() {
        writer.upsert(documentFor("site:acme"));
        final List<IndexDocument> snapshot = writer.upserts();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(documentFor("site:beta")));
    }

    @Test
    void deletesSnapshotIsImmutable() {
        writer.delete(IndexDocumentId.of("site:acme"));
        final List<IndexDocumentId> snapshot = writer.deletes();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(IndexDocumentId.of("site:beta")));
    }

    // --- clear ---

    @Test
    void clearRemovesAllRecordedOperations() {
        writer.upsert(documentFor("site:acme"));
        writer.delete(IndexDocumentId.of("site:beta"));
        writer.clear();
        assertTrue(writer.upserts().isEmpty());
        assertTrue(writer.deletes().isEmpty());
    }

    // --- insertion order ---

    @Test
    void preservesUpsertInsertionOrder() {
        final IndexDocument first = documentFor("site:acme");
        final IndexDocument second = documentFor("site:beta");
        final IndexDocument third = documentFor("site:gamma");
        writer.upsert(first);
        writer.upsert(second);
        writer.upsert(third);
        assertEquals(List.of(first, second, third), writer.upserts());
    }

    @Test
    void preservesDeleteInsertionOrder() {
        final IndexDocumentId first = IndexDocumentId.of("site:acme");
        final IndexDocumentId second = IndexDocumentId.of("site:beta");
        final IndexDocumentId third = IndexDocumentId.of("site:gamma");
        writer.delete(first);
        writer.delete(second);
        writer.delete(third);
        assertEquals(List.of(first, second, third), writer.deletes());
    }
}
