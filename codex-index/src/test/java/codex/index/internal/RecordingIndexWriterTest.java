package codex.index.internal;

import codex.codex.api.model.identity.SiteKey;
import codex.index.api.IndexDocument;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RecordingIndexWriter}.
 */
class RecordingIndexWriterTest {

    private static final IndexDocumentId ID_A = IndexDocumentId.of("content-item:acme:blog-post:post-a");
    private static final IndexDocumentId ID_B = IndexDocumentId.of("content-item:acme:blog-post:post-b");

    private RecordingIndexWriter writer;

    @BeforeEach
    void setUp() {
        writer = new RecordingIndexWriter();
    }

    private IndexDocument documentWith(final IndexDocumentId id) {
        return IndexDocument.builder()
                .id(id)
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
    void recordsUpserts() {
        writer.upsert(documentWith(ID_A));
        writer.upsert(documentWith(ID_B));
        assertEquals(2, writer.upserts().size());
        assertEquals(ID_A, writer.upserts().get(0).id());
        assertEquals(ID_B, writer.upserts().get(1).id());
    }

    @Test
    void recordsDeletes() {
        writer.delete(ID_A);
        writer.delete(ID_B);
        assertEquals(2, writer.deletes().size());
        assertEquals(ID_A, writer.deletes().get(0));
        assertEquals(ID_B, writer.deletes().get(1));
    }

    @Test
    void upsertsSnapshotIsImmutable() {
        writer.upsert(documentWith(ID_A));
        assertThrows(UnsupportedOperationException.class,
                () -> writer.upserts().add(documentWith(ID_B)));
    }

    @Test
    void deletesSnapshotIsImmutable() {
        writer.delete(ID_A);
        assertThrows(UnsupportedOperationException.class,
                () -> writer.deletes().add(ID_B));
    }

    @Test
    void clearResetsAllRecordings() {
        writer.upsert(documentWith(ID_A));
        writer.delete(ID_B);
        writer.clear();
        assertTrue(writer.upserts().isEmpty());
        assertTrue(writer.deletes().isEmpty());
    }

    @Test
    void initialStateIsEmpty() {
        assertTrue(writer.upserts().isEmpty());
        assertTrue(writer.deletes().isEmpty());
    }
}
