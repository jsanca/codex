package codex.chronicon.internal;

import codex.chronicon.api.AuditRecordId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AuditRecordIdGenerator}.
 */
class AuditRecordIdGeneratorTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    @Test
    void generatesExpectedIdForCreated() {
        final AuditRecordId id = AuditRecordIdGenerator.contentItemLifecycle(
                "created", SITE_KEY, CT_KEY, ITEM_KEY, NOW);
        assertEquals(
                "audit:content-item-created:acme:blog-post:my-post:" + NOW.toEpochMilli(),
                id.value());
    }

    @Test
    void generatesExpectedIdForUpdated() {
        final AuditRecordId id = AuditRecordIdGenerator.contentItemLifecycle(
                "updated", SITE_KEY, CT_KEY, ITEM_KEY, NOW);
        assertEquals(
                "audit:content-item-updated:acme:blog-post:my-post:" + NOW.toEpochMilli(),
                id.value());
    }

    @Test
    void generatesExpectedIdForDeleted() {
        final AuditRecordId id = AuditRecordIdGenerator.contentItemLifecycle(
                "deleted", SITE_KEY, CT_KEY, ITEM_KEY, NOW);
        assertEquals(
                "audit:content-item-deleted:acme:blog-post:my-post:" + NOW.toEpochMilli(),
                id.value());
    }

    @Test
    void rejectsNullAction() {
        assertThrows(NullPointerException.class, () ->
                AuditRecordIdGenerator.contentItemLifecycle(null, SITE_KEY, CT_KEY, ITEM_KEY, NOW));
    }

    @Test
    void rejectsNullSiteKey() {
        assertThrows(NullPointerException.class, () ->
                AuditRecordIdGenerator.contentItemLifecycle("created", null, CT_KEY, ITEM_KEY, NOW));
    }

    @Test
    void rejectsNullContentTypeKey() {
        assertThrows(NullPointerException.class, () ->
                AuditRecordIdGenerator.contentItemLifecycle("created", SITE_KEY, null, ITEM_KEY, NOW));
    }

    @Test
    void rejectsNullContentItemKey() {
        assertThrows(NullPointerException.class, () ->
                AuditRecordIdGenerator.contentItemLifecycle("created", SITE_KEY, CT_KEY, null, NOW));
    }

    @Test
    void rejectsNullOccurredAt() {
        assertThrows(NullPointerException.class, () ->
                AuditRecordIdGenerator.contentItemLifecycle("created", SITE_KEY, CT_KEY, ITEM_KEY, null));
    }
}
