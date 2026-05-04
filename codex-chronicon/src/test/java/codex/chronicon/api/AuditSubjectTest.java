package codex.chronicon.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AuditSubject}.
 */
class AuditSubjectTest {

    @Test
    void rejectsNullType() {
        assertThrows(NullPointerException.class, () -> AuditSubject.of(null, "id-1"));
    }

    @Test
    void rejectsBlankType() {
        assertThrows(IllegalArgumentException.class, () -> AuditSubject.of("  ", "id-1"));
    }

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class, () -> AuditSubject.of("site", null));
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> AuditSubject.of("site", "  "));
    }

    @Test
    void trimsType() {
        assertEquals("site", AuditSubject.of("  site  ", "id-1").type());
    }

    @Test
    void trimsId() {
        assertEquals("id-1", AuditSubject.of("site", "  id-1  ").id());
    }

    @Test
    void trimsKey() {
        assertEquals("acme", AuditSubject.of("site", "id-1", "  acme  ").key());
    }

    @Test
    void allowsNullKey() {
        final AuditSubject subject = AuditSubject.of("site", "id-1");
        assertNull(subject.key());
    }

    @Test
    void twoArgFactoryNullKey() {
        final AuditSubject subject = AuditSubject.of("content-item", "item-123");
        assertEquals("content-item", subject.type());
        assertEquals("item-123", subject.id());
        assertNull(subject.key());
    }

    @Test
    void threeArgFactoryWorks() {
        final AuditSubject subject = AuditSubject.of("content-item", "item-123", "my-post");
        assertEquals("content-item", subject.type());
        assertEquals("item-123", subject.id());
        assertEquals("my-post", subject.key());
    }

    @Test
    void equalityByAllFields() {
        final AuditSubject a = AuditSubject.of("site", "id-1", "acme");
        final AuditSubject b = AuditSubject.of("site", "id-1", "acme");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentTypeNotEqual() {
        assertNotEquals(
                AuditSubject.of("site", "id-1"),
                AuditSubject.of("content-item", "id-1"));
    }
}
