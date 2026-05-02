package codex.codex.api.model.value;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentRevisionStatusTest {

    @Test
    void expectedStatusesExist() {
        assertEquals("WORKING", ContentRevisionStatus.WORKING.name());
        assertEquals("PUBLISHED", ContentRevisionStatus.PUBLISHED.name());
        assertEquals("ARCHIVED", ContentRevisionStatus.ARCHIVED.name());
    }

    @Test
    void hasExactlyThreeValues() {
        assertEquals(3, ContentRevisionStatus.values().length);
    }
}
