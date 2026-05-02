package codex.codex.api.model.event;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemCreatedEvent}.
 */
class ContentItemEventTest {

    private static final ContentItemId ID = ContentItemId.of("content-item:acme:blog-post:my-post");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId VERSION_ID = ContentTypeVersionId.of("content-type-version:acme:blog-post:v1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final Actor ACTOR = Actor.system("test");
    private static final Instant NOW = Instant.parse("2026-05-02T10:00:00Z");

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemCreatedEvent(null, SITE_KEY, CT_KEY, VERSION_ID, ITEM_KEY, ACTOR, NOW));
    }

    @Test
    void rejectsNullSiteKey() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemCreatedEvent(ID, null, CT_KEY, VERSION_ID, ITEM_KEY, ACTOR, NOW));
    }

    @Test
    void rejectsNullContentTypeKey() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemCreatedEvent(ID, SITE_KEY, null, VERSION_ID, ITEM_KEY, ACTOR, NOW));
    }

    @Test
    void rejectsNullContentTypeVersionId() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemCreatedEvent(ID, SITE_KEY, CT_KEY, null, ITEM_KEY, ACTOR, NOW));
    }

    @Test
    void rejectsNullKey() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemCreatedEvent(ID, SITE_KEY, CT_KEY, VERSION_ID, null, ACTOR, NOW));
    }

    @Test
    void rejectsNullActor() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemCreatedEvent(ID, SITE_KEY, CT_KEY, VERSION_ID, ITEM_KEY, null, NOW));
    }

    @Test
    void rejectsNullOccurredAt() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemCreatedEvent(ID, SITE_KEY, CT_KEY, VERSION_ID, ITEM_KEY, ACTOR, null));
    }

    @Test
    void occurredAtReturnedFromCodexEvent() {
        final ContentItemCreatedEvent event =
                new ContentItemCreatedEvent(ID, SITE_KEY, CT_KEY, VERSION_ID, ITEM_KEY, ACTOR, NOW);
        assertEquals(NOW, event.occurredAt());
    }
}
