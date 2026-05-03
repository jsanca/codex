package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PublishContentItemCommand}.
 */
class PublishContentItemCommandTest {

    @Test
    void requiresNonNullSiteKey() {
        assertThrows(NullPointerException.class,
                () -> PublishContentItemCommand.of(
                        (SiteKey) null,
                        ContentTypeKey.of("blog-post"),
                        ContentItemKey.of("my-post")));
    }

    @Test
    void requiresNonNullContentTypeKey() {
        assertThrows(NullPointerException.class,
                () -> PublishContentItemCommand.of(
                        SiteKey.of("acme"),
                        (ContentTypeKey) null,
                        ContentItemKey.of("my-post")));
    }

    @Test
    void requiresNonNullKey() {
        assertThrows(NullPointerException.class,
                () -> PublishContentItemCommand.of(
                        SiteKey.of("acme"),
                        ContentTypeKey.of("blog-post"),
                        (ContentItemKey) null));
    }

    @Test
    void typedFactoryWorks() {
        final PublishContentItemCommand command = PublishContentItemCommand.of(
                SiteKey.of("acme"),
                ContentTypeKey.of("blog-post"),
                ContentItemKey.of("my-post"));

        assertEquals(SiteKey.of("acme"), command.siteKey());
        assertEquals(ContentTypeKey.of("blog-post"), command.contentTypeKey());
        assertEquals(ContentItemKey.of("my-post"), command.key());
    }

    @Test
    void stringFactoryWorks() {
        final PublishContentItemCommand command = PublishContentItemCommand.of(
                "acme", "blog-post", "my-post");

        assertEquals(SiteKey.of("acme"), command.siteKey());
        assertEquals(ContentTypeKey.of("blog-post"), command.contentTypeKey());
        assertEquals(ContentItemKey.of("my-post"), command.key());
    }
}
