package codex.codex.api.model.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentRevisionIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(NullPointerException.class, () -> new ContentRevisionId(null));
        assertThrows(NullPointerException.class, () -> ContentRevisionId.of(null));
    }

    @Test
    void rejectsBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new ContentRevisionId("   "));
        assertThrows(IllegalArgumentException.class, () -> ContentRevisionId.of(""));
    }

    @Test
    void trimsValue() {
        final ContentRevisionId id = ContentRevisionId.of("  my-id  ");
        assertEquals("my-id", id.value());
    }

    @Test
    void ofFactoryWorks() {
        final ContentRevisionId id = ContentRevisionId.of("some-revision");
        assertEquals("some-revision", id.value());
    }

    @Test
    void deterministicFactoryCreatesCorrectId() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("hello-world");

        final ContentRevisionId id = ContentRevisionId.forRevision(siteKey, contentTypeKey, itemKey, 1);
        assertEquals("content-revision:acme:blog-post:hello-world:r1", id.value());
    }

    @Test
    void deterministicFactoryRejectsNulls() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("hello-world");

        assertThrows(NullPointerException.class, () ->
                ContentRevisionId.forRevision(null, contentTypeKey, itemKey, 1));
        assertThrows(NullPointerException.class, () ->
                ContentRevisionId.forRevision(siteKey, null, itemKey, 1));
        assertThrows(NullPointerException.class, () ->
                ContentRevisionId.forRevision(siteKey, contentTypeKey, null, 1));
    }

    @Test
    void deterministicFactoryRejectsInvalidRevisionNumber() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("hello-world");

        assertThrows(IllegalArgumentException.class, () ->
                ContentRevisionId.forRevision(siteKey, contentTypeKey, itemKey, 0));
        assertThrows(IllegalArgumentException.class, () ->
                ContentRevisionId.forRevision(siteKey, contentTypeKey, itemKey, -1));
    }
}
