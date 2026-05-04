package codex.chronicon.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AuditRecordId}.
 */
class AuditRecordIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(NullPointerException.class, () -> AuditRecordId.of(null));
    }

    @Test
    void rejectsBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> AuditRecordId.of("   "));
    }

    @Test
    void trimsValue() {
        assertEquals("audit-001", AuditRecordId.of("  audit-001  ").value());
    }

    @Test
    void ofFactoryWorks() {
        final AuditRecordId id = AuditRecordId.of("audit-001");
        assertNotNull(id);
        assertEquals("audit-001", id.value());
    }

    @Test
    void equalityByValue() {
        final AuditRecordId a = AuditRecordId.of("audit-001");
        final AuditRecordId b = AuditRecordId.of("audit-001");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentValueNotEqual() {
        assertNotEquals(AuditRecordId.of("audit-001"), AuditRecordId.of("audit-002"));
    }

    @Test
    void toStringReturnsValue() {
        assertEquals("audit-001", AuditRecordId.of("audit-001").toString());
    }
}
