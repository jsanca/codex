package codex.index.internal;

import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.observance.InMemoryObservance;
import codex.index.api.IndexDocument;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexResourceType;
import codex.index.api.IndexWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObservingIndexWriterTest {

    private RecordingIndexWriter delegate;
    private InMemoryObservance observance;
    private ObservingIndexWriter writer;

    @BeforeEach
    void setUp() {
        delegate = new RecordingIndexWriter();
        observance = new InMemoryObservance();
        writer = new ObservingIndexWriter(delegate, observance);
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(NullPointerException.class,
                () -> new ObservingIndexWriter(null, observance));
    }

    @Test
    void constructorRejectsNullObservance() {
        assertThrows(NullPointerException.class,
                () -> new ObservingIndexWriter(delegate, null));
    }

    // --- upsert success ---

    @Test
    void upsertIncrementsCalls() {
        final IndexDocument doc = testDocument("doc-1");
        writer.upsert(doc);
        assertEquals(1, observance.counterValue("index.upsert.calls"));
    }

    @Test
    void upsertRecordsDuration() {
        writer.upsert(testDocument("doc-1"));
        assertEquals(1, observance.timerCount("index.upsert.duration"));
    }

    @Test
    void upsertDelegatesDocument() {
        final IndexDocument doc = testDocument("doc-2");
        writer.upsert(doc);
        assertEquals(List.of(doc), delegate.upserted);
    }

    // --- upsert failure ---

    @Test
    void upsertFailureIncrementsFailureCounter() {
        delegate.failOnUpsert = true;
        assertThrows(RuntimeException.class, () -> writer.upsert(testDocument("doc-3")));
        assertEquals(1, observance.counterValue("index.upsert.failed"));
    }

    @Test
    void upsertFailureStillRecordsDuration() {
        delegate.failOnUpsert = true;
        assertThrows(RuntimeException.class, () -> writer.upsert(testDocument("doc-4")));
        assertEquals(1, observance.timerCount("index.upsert.duration"));
    }

    @Test
    void upsertExceptionPropagates() {
        delegate.failOnUpsert = true;
        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> writer.upsert(testDocument("doc-5")));
        assertEquals("upsert failed", ex.getMessage());
    }

    // --- delete success ---

    @Test
    void deleteIncrementsCalls() {
        writer.delete(IndexDocumentId.of("doc-1"));
        assertEquals(1, observance.counterValue("index.delete.calls"));
    }

    @Test
    void deleteRecordsDuration() {
        writer.delete(IndexDocumentId.of("doc-1"));
        assertEquals(1, observance.timerCount("index.delete.duration"));
    }

    @Test
    void deleteDelegatesId() {
        final IndexDocumentId id = IndexDocumentId.of("doc-6");
        writer.delete(id);
        assertEquals(List.of(id), delegate.deleted);
    }

    // --- delete failure ---

    @Test
    void deleteFailureIncrementsFailureCounter() {
        delegate.failOnDelete = true;
        assertThrows(RuntimeException.class, () -> writer.delete(IndexDocumentId.of("doc-7")));
        assertEquals(1, observance.counterValue("index.delete.failed"));
    }

    @Test
    void deleteFailureStillRecordsDuration() {
        delegate.failOnDelete = true;
        assertThrows(RuntimeException.class, () -> writer.delete(IndexDocumentId.of("doc-8")));
        assertEquals(1, observance.timerCount("index.delete.duration"));
    }

    @Test
    void deleteExceptionPropagates() {
        delegate.failOnDelete = true;
        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> writer.delete(IndexDocumentId.of("doc-9")));
        assertEquals("delete failed", ex.getMessage());
    }

    // --- helpers ---

    private static IndexDocument testDocument(final String id) {
        return IndexDocument.builder()
                .id(IndexDocumentId.of(id))
                .resourceType(IndexResourceType.CONTENT_ITEM)
                .siteKey(SiteKey.of("test"))
                .build();
    }

    private static final class RecordingIndexWriter implements IndexWriter {

        final List<IndexDocument> upserted = new ArrayList<>();
        final List<IndexDocumentId> deleted = new ArrayList<>();
        boolean failOnUpsert = false;
        boolean failOnDelete = false;

        @Override
        public void upsert(final IndexDocument document) {
            if (failOnUpsert) {
                throw new RuntimeException("upsert failed");
            }
            upserted.add(document);
        }

        @Override
        public void delete(final IndexDocumentId id) {
            if (failOnDelete) {
                throw new RuntimeException("delete failed");
            }
            deleted.add(id);
        }
    }
}
