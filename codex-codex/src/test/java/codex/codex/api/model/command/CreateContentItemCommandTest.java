package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CreateContentItemCommand}.
 */
class CreateContentItemCommandTest {

    @Test
    void requiresNonNullSiteKey() {
        assertThrows(NullPointerException.class,
                () -> CreateContentItemCommand.of(
                        (SiteKey) null,
                        ContentTypeKey.of("blog-post"),
                        ContentItemKey.of("my-post"),
                        null));
    }

    @Test
    void requiresNonNullContentTypeKey() {
        assertThrows(NullPointerException.class,
                () -> CreateContentItemCommand.of(
                        SiteKey.of("acme"),
                        (ContentTypeKey) null,
                        ContentItemKey.of("my-post"),
                        null));
    }

    @Test
    void requiresNonNullKey() {
        assertThrows(NullPointerException.class,
                () -> CreateContentItemCommand.of(
                        SiteKey.of("acme"),
                        ContentTypeKey.of("blog-post"),
                        (ContentItemKey) null,
                        null));
    }

    @Test
    void defaultsValuesToEmptyMapWhenNull() {
        final CreateContentItemCommand command = CreateContentItemCommand.of(
                SiteKey.of("acme"),
                ContentTypeKey.of("blog-post"),
                ContentItemKey.of("my-post"),
                null);

        assertNotNull(command.values());
        assertTrue(command.values().isEmpty());
    }

    @Test
    void defensivelyCopiesValues() {
        final Map<FieldKey, Object> mutable = new HashMap<>();
        mutable.put(FieldKey.of("title"), "Hello");
        final CreateContentItemCommand command = CreateContentItemCommand.of(
                SiteKey.of("acme"),
                ContentTypeKey.of("blog-post"),
                ContentItemKey.of("my-post"),
                mutable);

        mutable.put(FieldKey.of("summary"), "Extra");
        assertEquals(1, command.values().size());
    }

    @Test
    void valuesAccessorIsImmutable() {
        final CreateContentItemCommand command = CreateContentItemCommand.of(
                SiteKey.of("acme"),
                ContentTypeKey.of("blog-post"),
                ContentItemKey.of("my-post"),
                Map.of(FieldKey.of("title"), "Hello"));

        assertThrows(UnsupportedOperationException.class,
                () -> command.values().put(FieldKey.of("other"), "x"));
    }

    @Test
    void rejectsNullValueMapKey() {
        final Map<FieldKey, Object> badValues = new HashMap<>();
        badValues.put(null, "some-value");
        assertThrows(NullPointerException.class,
                () -> CreateContentItemCommand.of(
                        SiteKey.of("acme"),
                        ContentTypeKey.of("blog-post"),
                        ContentItemKey.of("my-post"),
                        badValues));
    }

    @Test
    void rejectsNullValueMapValue() {
        final Map<FieldKey, Object> badValues = new HashMap<>();
        badValues.put(FieldKey.of("title"), null);
        assertThrows(NullPointerException.class,
                () -> CreateContentItemCommand.of(
                        SiteKey.of("acme"),
                        ContentTypeKey.of("blog-post"),
                        ContentItemKey.of("my-post"),
                        badValues));
    }

    @Test
    void typedFactoryWorks() {
        final CreateContentItemCommand command = CreateContentItemCommand.of(
                SiteKey.of("acme"),
                ContentTypeKey.of("blog-post"),
                ContentItemKey.of("my-post"),
                Map.of(FieldKey.of("title"), "Welcome"));

        assertEquals(SiteKey.of("acme"), command.siteKey());
        assertEquals(ContentTypeKey.of("blog-post"), command.contentTypeKey());
        assertEquals(ContentItemKey.of("my-post"), command.key());
        assertEquals("Welcome", command.values().get(FieldKey.of("title")));
    }

    @Test
    void stringFactoryWorks() {
        final CreateContentItemCommand command = CreateContentItemCommand.of(
                "acme",
                "blog-post",
                "my-post",
                Map.of(FieldKey.of("title"), "Hello"));

        assertEquals(SiteKey.of("acme"), command.siteKey());
        assertEquals(ContentTypeKey.of("blog-post"), command.contentTypeKey());
        assertEquals(ContentItemKey.of("my-post"), command.key());
    }
}
