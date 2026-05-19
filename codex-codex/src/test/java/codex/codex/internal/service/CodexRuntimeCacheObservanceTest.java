package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.runtime.CodexRuntime;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.fundamentum.api.observance.InMemoryObservance;
import codex.fundamentum.api.observance.Observance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying that the {@link CodexRuntime} content-item cache is wired
 * to the supplied {@link Observance} and records hit/miss counters for
 * {@link codex.fundamentum.api.cache.ObservingCacheRegion} operations.
 *
 * <p>Tests use a non-existent key so that no domain setup is required and no async
 * invalidation events can interfere with cache state.</p>
 */
class CodexRuntimeCacheObservanceTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("article");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("nonexistent-item");

    // --- factory ---

    @Test
    void noArgFactoryStillWorks() {
        try (final CodexRuntime runtime = CodexRuntime.inMemory()) {
            assertNotNull(runtime);
        }
    }

    @Test
    void inMemoryWithObservanceCreatesRuntime() {
        try (final CodexRuntime runtime = CodexRuntime.inMemory(new InMemoryObservance())) {
            assertNotNull(runtime);
        }
    }

    // --- getOrLoad.miss ---

    @Test
    void firstFindByKeyRecordsGetOrLoadMiss() {
        final InMemoryObservance observance = new InMemoryObservance();
        try (final CodexRuntime runtime = CodexRuntime.inMemory(observance)) {

            runtime.contentItemService().findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

            assertEquals(1L, observance.counterValue("cache.contentItem.getOrLoad.miss"));
            assertEquals(0L, observance.counterValue("cache.contentItem.getOrLoad.hit"));
        }
    }

    @Test
    void firstFindByKeyReturnsMiss() {
        try (final CodexRuntime runtime = CodexRuntime.inMemory(new InMemoryObservance())) {
            assertTrue(runtime.contentItemService()
                    .findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR).isEmpty());
        }
    }

    // --- getOrLoad.hit ---

    @Test
    void secondFindByKeyRecordsGetOrLoadHit() {
        final InMemoryObservance observance = new InMemoryObservance();
        try (final CodexRuntime runtime = CodexRuntime.inMemory(observance)) {

            // first call: miss — NotFound entry cached
            runtime.contentItemService().findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);
            // second call: hit — NotFound returned from cache
            runtime.contentItemService().findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

            assertEquals(1L, observance.counterValue("cache.contentItem.getOrLoad.miss"));
            assertEquals(1L, observance.counterValue("cache.contentItem.getOrLoad.hit"));
        }
    }

    @Test
    void hitPreservesNotFoundBehavior() {
        try (final CodexRuntime runtime = CodexRuntime.inMemory(new InMemoryObservance())) {
            runtime.contentItemService().findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

            // second call comes from cache — result must still be empty
            assertTrue(runtime.contentItemService()
                    .findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR).isEmpty());
        }
    }

    // --- no-op observance: runtime still functions ---

    @Test
    void noOpObservanceDoesNotBreakCacheOperations() {
        try (final CodexRuntime runtime = CodexRuntime.inMemory(Observance.noop())) {

            runtime.contentItemService().findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);
            runtime.contentItemService().findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

            // no assertion — just verify no exception is thrown
        }
    }

    // --- distinct keys accumulate independently ---

    @Test
    void distinctKeysMissCounterAccumulates() {
        final InMemoryObservance observance = new InMemoryObservance();
        try (final CodexRuntime runtime = CodexRuntime.inMemory(observance)) {

            runtime.contentItemService().findByKey(SITE_KEY, CT_KEY,
                    ContentItemKey.of("item-a"), ACTOR);
            runtime.contentItemService().findByKey(SITE_KEY, CT_KEY,
                    ContentItemKey.of("item-b"), ACTOR);

            assertEquals(2L, observance.counterValue("cache.contentItem.getOrLoad.miss"));
            assertEquals(0L, observance.counterValue("cache.contentItem.getOrLoad.hit"));
        }
    }
}
