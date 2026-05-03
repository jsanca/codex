package codex.codex.api.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IndexResourceType}.
 */
class IndexResourceTypeTest {

    @Test
    void siteValueExists() {
        assertNotNull(IndexResourceType.SITE);
    }

    @Test
    void contentTypeValueExists() {
        assertNotNull(IndexResourceType.CONTENT_TYPE);
    }

    @Test
    void contentTypeVersionValueExists() {
        assertNotNull(IndexResourceType.CONTENT_TYPE_VERSION);
    }

    @Test
    void contentItemValueExists() {
        assertNotNull(IndexResourceType.CONTENT_ITEM);
    }

    @Test
    void contentRevisionValueExists() {
        assertNotNull(IndexResourceType.CONTENT_REVISION);
    }

    @Test
    void allFiveValuesPresent() {
        assertEquals(5, IndexResourceType.values().length);
    }
}
