package codex.chronicon.internal;

import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.event.ContentTypeCreatedEvent;
import codex.codex.api.model.event.SiteCreatedEvent;
import codex.codex.api.model.event.SiteStartedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test proving Chronicon subscribers work with {@link LocalCodexEventDispatcher}.
 *
 * <p>Dispatches domain events manually and asserts that audit records are saved.
 * No {@code CodexRuntime}, no indexing, no Spring.</p>
 */
class ChroniconDispatcherIntegrationTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final SiteId SITE_ID = SiteId.of("site-id-1");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeId CT_ID = ContentTypeId.of("ct-id-1");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.of("rev-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Actor ACTOR = Actor.human(ACTOR_ID, "Test User");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private RecordingChroniconRepository repository;
    private LocalCodexEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        repository = new RecordingChroniconRepository();

        final List<CodexEventSubscriber<?>> subscribers = List.of(
                new SiteCreatedChroniconSubscriber(repository),
                new ContentTypeCreatedChroniconSubscriber(repository),
                new ContentItemPublishedChroniconSubscriber(repository));

        dispatcher = new LocalCodexEventDispatcher(subscribers);
    }

    @Test
    void dispatchingAllThreeEventsSavesThreeAuditRecords() {
        dispatcher.dispatch(new SiteCreatedEvent(SITE_ID, SITE_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentTypeCreatedEvent(CT_ID, SITE_KEY, CT_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, REVISION_ID, ACTOR, NOW));

        assertEquals(3, repository.savedRecords().size());
    }

    @Test
    void unsupportedEventIsIgnored() {
        final SiteStartedEvent unsupported = new SiteStartedEvent(SITE_ID, SITE_KEY, ACTOR, NOW);
        dispatcher.dispatch(unsupported);

        assertEquals(0, repository.savedRecords().size());
    }

    @Test
    void siteCreatedEventSavesRecordWithSiteSubjectType() {
        dispatcher.dispatch(new SiteCreatedEvent(SITE_ID, SITE_KEY, ACTOR, NOW));

        assertEquals(1, repository.savedRecords().size());
        assertEquals("site", repository.savedRecords().get(0).subject().type());
    }

    @Test
    void contentTypeCreatedEventSavesRecordWithContentTypeSubjectType() {
        dispatcher.dispatch(new ContentTypeCreatedEvent(CT_ID, SITE_KEY, CT_KEY, ACTOR, NOW));

        assertEquals(1, repository.savedRecords().size());
        assertEquals("content-type", repository.savedRecords().get(0).subject().type());
    }

    @Test
    void contentItemPublishedEventSavesRecordWithContentItemSubjectType() {
        dispatcher.dispatch(new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, REVISION_ID, ACTOR, NOW));

        assertEquals(1, repository.savedRecords().size());
        assertEquals("content-item", repository.savedRecords().get(0).subject().type());
    }
}
