package codex.codex.api.model.event;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentTypeEventTest {

    private final ContentTypeId id = ContentTypeId.generate();
    private final SiteKey siteKey = SiteKey.of("acme");
    private final ContentTypeKey key = ContentTypeKey.of("blog-post");
    private final Actor actor = Actor.system("test");
    private final Instant now = Instant.now();

    // --- ContentTypeCreatedEvent ---

    @Test
    @DisplayName("ContentTypeCreatedEvent rejects null id")
    void createdEventRejectsNullId() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeCreatedEvent(null, siteKey, key, actor, now));
    }

    @Test
    @DisplayName("ContentTypeCreatedEvent rejects null siteKey")
    void createdEventRejectsNullSiteKey() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeCreatedEvent(id, null, key, actor, now));
    }

    @Test
    @DisplayName("ContentTypeCreatedEvent rejects null key")
    void createdEventRejectsNullKey() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeCreatedEvent(id, siteKey, null, actor, now));
    }

    @Test
    @DisplayName("ContentTypeCreatedEvent rejects null actor")
    void createdEventRejectsNullActor() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeCreatedEvent(id, siteKey, key, null, now));
    }

    @Test
    @DisplayName("ContentTypeCreatedEvent rejects null occurredAt")
    void createdEventRejectsNullOccurredAt() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeCreatedEvent(id, siteKey, key, actor, null));
    }

    // --- ContentTypeActivatedEvent ---

    @Test
    @DisplayName("ContentTypeActivatedEvent rejects null previousStatus")
    void activatedEventRejectsNullPreviousStatus() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeActivatedEvent(id, siteKey, key, null, ContentTypeStatus.ACTIVE, actor, now));
    }

    @Test
    @DisplayName("ContentTypeActivatedEvent rejects null newStatus")
    void activatedEventRejectsNullNewStatus() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeActivatedEvent(id, siteKey, key, ContentTypeStatus.DRAFT, null, actor, now));
    }

    // --- ContentTypeArchivedEvent ---

    @Test
    @DisplayName("ContentTypeArchivedEvent rejects null previousStatus")
    void archivedEventRejectsNullPreviousStatus() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeArchivedEvent(id, siteKey, key, null, ContentTypeStatus.ARCHIVED, actor, now));
    }

    @Test
    @DisplayName("ContentTypeArchivedEvent rejects null newStatus")
    void archivedEventRejectsNullNewStatus() {
        assertThrows(NullPointerException.class, () ->
                new ContentTypeArchivedEvent(id, siteKey, key, ContentTypeStatus.DRAFT, null, actor, now));
    }
}
