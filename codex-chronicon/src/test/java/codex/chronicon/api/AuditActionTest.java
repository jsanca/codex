package codex.chronicon.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AuditAction}.
 */
class AuditActionTest {

    @Test
    void createdExists() {
        assertNotNull(AuditAction.CREATED);
    }

    @Test
    void updatedExists() {
        assertNotNull(AuditAction.UPDATED);
    }

    @Test
    void publishedExists() {
        assertNotNull(AuditAction.PUBLISHED);
    }

    @Test
    void archivedExists() {
        assertNotNull(AuditAction.ARCHIVED);
    }

    @Test
    void startedExists() {
        assertNotNull(AuditAction.STARTED);
    }

    @Test
    void suspendedExists() {
        assertNotNull(AuditAction.SUSPENDED);
    }

    @Test
    void activatedExists() {
        assertNotNull(AuditAction.ACTIVATED);
    }

    @Test
    void deactivatedExists() {
        assertNotNull(AuditAction.DEACTIVATED);
    }

    @Test
    void deletedExists() {
        assertNotNull(AuditAction.DELETED);
    }

    @Test
    void unknownExists() {
        assertNotNull(AuditAction.UNKNOWN);
    }

    @Test
    void tenValuesPresent() {
        assertEquals(10, AuditAction.values().length);
    }
}
