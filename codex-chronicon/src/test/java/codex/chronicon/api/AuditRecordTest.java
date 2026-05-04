package codex.chronicon.api;

import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AuditRecord}.
 */
class AuditRecordTest {

    private static final AuditRecordId ID = AuditRecordId.of("audit-001");
    private static final AuditAction ACTION = AuditAction.CREATED;
    private static final AuditSubject SUBJECT = AuditSubject.of("site", "site-id-1", "acme");
    private static final ActorId ACTOR = ActorId.of("user-1");
    private static final Instant NOW = Instant.parse("2026-05-04T09:00:00Z");

    private AuditRecord.Builder minimal() {
        return AuditRecord.builder()
                .id(ID)
                .action(ACTION)
                .subject(SUBJECT)
                .actorId(ACTOR)
                .occurredAt(NOW);
    }

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class, () -> minimal().id(null).build());
    }

    @Test
    void rejectsNullAction() {
        assertThrows(NullPointerException.class, () -> minimal().action(null).build());
    }

    @Test
    void rejectsNullSubject() {
        assertThrows(NullPointerException.class, () -> minimal().subject(null).build());
    }

    @Test
    void rejectsNullActorId() {
        assertThrows(NullPointerException.class, () -> minimal().actorId(null).build());
    }

    @Test
    void rejectsNullOccurredAt() {
        assertThrows(NullPointerException.class, () -> minimal().occurredAt(null).build());
    }

    @Test
    void defaultsNullSummaryToEmptyString() {
        assertEquals("", minimal().summary(null).build().summary());
    }

    @Test
    void trimsSummary() {
        assertEquals("site created", minimal().summary("  site created  ").build().summary());
    }

    @Test
    void defaultsNullMetadataToEmptyMap() {
        assertTrue(minimal().metadata(null).build().metadata().isEmpty());
    }

    @Test
    void rejectsNullMetadataKey() {
        final Map<String, String> bad = new HashMap<>();
        bad.put(null, "value");
        assertThrows(NullPointerException.class, () -> minimal().metadata(bad).build());
    }

    @Test
    void rejectsBlankMetadataKey() {
        assertThrows(IllegalArgumentException.class, () ->
                minimal().metadata(Map.of("  ", "value")).build());
    }

    @Test
    void trimsMetadataKeys() {
        final Map<String, String> input = new HashMap<>();
        input.put("  key  ", "value");
        final AuditRecord record = minimal().metadata(input).build();
        assertTrue(record.metadata().containsKey("key"));
        assertFalse(record.metadata().containsKey("  key  "));
    }

    @Test
    void rejectsNullMetadataValue() {
        final Map<String, String> bad = new HashMap<>();
        bad.put("key", null);
        assertThrows(NullPointerException.class, () -> minimal().metadata(bad).build());
    }

    @Test
    void metadataAccessorIsImmutable() {
        final AuditRecord record = minimal().metadata(Map.of("k", "v")).build();
        assertThrows(UnsupportedOperationException.class, () -> record.metadata().put("x", "y"));
    }

    @Test
    void defensivelyCopiesMetadata() {
        final Map<String, String> mutable = new HashMap<>();
        mutable.put("k", "v");
        final AuditRecord record = minimal().metadata(mutable).build();
        mutable.put("extra", "mutation");
        assertEquals(1, record.metadata().size());
    }

    @Test
    void builderSupportsAllFields() {
        final AuditRecord record = AuditRecord.builder()
                .id(ID)
                .action(ACTION)
                .subject(SUBJECT)
                .actorId(ACTOR)
                .occurredAt(NOW)
                .summary("Site acme was created")
                .metadata(Map.of("siteKey", "acme"))
                .build();
        assertEquals(ID, record.id());
        assertEquals(ACTION, record.action());
        assertEquals(SUBJECT, record.subject());
        assertEquals(ACTOR, record.actorId());
        assertEquals(NOW, record.occurredAt());
        assertEquals("Site acme was created", record.summary());
        assertEquals("acme", record.metadata().get("siteKey"));
    }

    @Test
    void copyOfPreservesAllFields() {
        final AuditRecord original = minimal()
                .summary("Original")
                .metadata(Map.of("k", "v"))
                .build();
        final AuditRecord copy = AuditRecord.copyOf(original).build();
        assertEquals(original.id(), copy.id());
        assertEquals(original.action(), copy.action());
        assertEquals(original.subject(), copy.subject());
        assertEquals(original.actorId(), copy.actorId());
        assertEquals(original.occurredAt(), copy.occurredAt());
        assertEquals(original.summary(), copy.summary());
        assertEquals(original.metadata(), copy.metadata());
    }

    @Test
    void toStringIncludesMetadataCount() {
        final AuditRecord record = minimal()
                .metadata(Map.of("a", "1", "b", "2"))
                .build();
        assertTrue(record.toString().contains("metadata.size=2"));
    }

    @Test
    void toStringDoesNotDumpMetadataContents() {
        final AuditRecord record = minimal()
                .metadata(Map.of("secretKey", "secretValue"))
                .build();
        assertFalse(record.toString().contains("secretValue"));
    }
}
