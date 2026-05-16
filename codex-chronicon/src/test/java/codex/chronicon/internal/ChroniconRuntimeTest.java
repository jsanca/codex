package codex.chronicon.internal;

import codex.chronicon.api.ChroniconRepository;
import codex.chronicon.api.runtime.ChroniconRuntime;
import codex.codex.api.model.event.ContentItemArchivedEvent;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.codex.api.model.event.ContentItemDeletedEvent;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.event.ContentItemRestoredEvent;
import codex.codex.api.model.event.ContentItemUnpublishedEvent;
import codex.codex.api.model.event.ContentItemUpdatedEvent;
import codex.codex.api.model.event.ContentTypeCreatedEvent;
import codex.codex.api.model.event.SiteCreatedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChroniconRuntime}.
 *
 * <p>Verifies assembly, subscriber composition, and dispatcher integration
 * without Spring, ServiceLoader, or durable persistence.</p>
 */
class ChroniconRuntimeTest {

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

    // --- factory: inMemory ---

    @Test
    void inMemoryCreatesRuntime() {
        final ChroniconRuntime runtime = ChroniconRuntime.inMemory();
        assertNotNull(runtime);
    }

    @Test
    void moduleNameReturnsCodexChronicon() {
        final ChroniconRuntime runtime = ChroniconRuntime.inMemory();
        assertEquals("codex-chronicon", runtime.moduleName());
    }

    @Test
    void repositoryIsNotNull() {
        final ChroniconRuntime runtime = ChroniconRuntime.inMemory();
        assertNotNull(runtime.repository());
    }

    @Test
    void inMemoryRepositoryIsMemoryBacked() {
        final ChroniconRuntime runtime = ChroniconRuntime.inMemory();
        assertInstanceOf(MemoryChroniconRepository.class, runtime.repository());
    }

    @Test
    void subscribersContainsNineSubscribers() {
        final ChroniconRuntime runtime = ChroniconRuntime.inMemory();
        assertEquals(9, runtime.subscribers().size());
    }

    @Test
    void subscribersListIsImmutable() {
        final ChroniconRuntime runtime = ChroniconRuntime.inMemory();
        final List<CodexEventSubscriber<? extends CodexEvent>> subscribers = runtime.subscribers();
        assertThrows(UnsupportedOperationException.class, () -> subscribers.add(null));
    }

    // --- factory: withRepository ---

    @Test
    void withRepositoryRejectsNull() {
        assertThrows(NullPointerException.class, () -> ChroniconRuntime.withRepository(null));
    }

    @Test
    void withRepositoryUsesProvidedRepository() {
        final RecordingChroniconRepository recording = new RecordingChroniconRepository();
        final ChroniconRuntime runtime = ChroniconRuntime.withRepository(recording);
        assertSame(recording, runtime.repository());
    }

    @Test
    void withRepositoryAlsoExposesNineSubscribers() {
        final RecordingChroniconRepository recording = new RecordingChroniconRepository();
        final ChroniconRuntime runtime = ChroniconRuntime.withRepository(recording);
        assertEquals(9, runtime.subscribers().size());
    }

    // --- close ---

    @Test
    void closeIsIdempotentAndSafe() {
        final ChroniconRuntime runtime = ChroniconRuntime.inMemory();
        assertDoesNotThrow(runtime::close);
        assertDoesNotThrow(runtime::close);
    }

    // --- dispatcher integration ---

    @Test
    void subscribersWorkWithLocalCodexEventDispatcher() {
        final RecordingChroniconRepository recording = new RecordingChroniconRepository();
        final ChroniconRuntime runtime = ChroniconRuntime.withRepository(recording);

        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(runtime.subscribers());

        dispatcher.dispatch(new SiteCreatedEvent(SITE_ID, SITE_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentTypeCreatedEvent(CT_ID, SITE_KEY, CT_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentItemCreatedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentItemUpdatedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, REVISION_ID, ACTOR, NOW));
        dispatcher.dispatch(new ContentItemUnpublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentItemArchivedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentItemRestoredEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, ACTOR, NOW));
        dispatcher.dispatch(new ContentItemDeletedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, ACTOR, NOW));

        assertEquals(9, recording.savedRecords().size(),
                "all nine events should produce audit records");
    }

    @Test
    void subscribersIgnoreUnknownEvents() {
        final RecordingChroniconRepository recording = new RecordingChroniconRepository();
        final ChroniconRuntime runtime = ChroniconRuntime.withRepository(recording);

        final LocalCodexEventDispatcher dispatcher =
                new LocalCodexEventDispatcher(runtime.subscribers());

        dispatcher.dispatch(new UnknownTestEvent(NOW));

        assertEquals(0, recording.savedRecords().size(),
                "unknown event should not produce any audit record");
    }

    // --- module name does not change after close ---

    @Test
    void moduleNameRemainsAvailableAfterClose() {
        final ChroniconRuntime runtime = ChroniconRuntime.inMemory();
        runtime.close();
        assertEquals("codex-chronicon", runtime.moduleName());
    }

    // --- inner types ---

    private record UnknownTestEvent(Instant occurredAt) implements CodexEvent {}
}
