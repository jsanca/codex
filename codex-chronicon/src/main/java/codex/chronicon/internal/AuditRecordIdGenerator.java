package codex.chronicon.internal;

import codex.chronicon.api.AuditRecordId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.time.Instant;
import java.util.Objects;

/**
 * Centralized factory for {@link AuditRecordId} values used by Chronicon subscribers.
 *
 * <p>Generates deterministic, human-readable identifiers from event fields so that
 * all ContentItem lifecycle audit records follow the same format. The published
 * lifecycle is excluded from this helper because its ID includes an additional
 * {@code publishedRevisionId} segment not present in the other events.</p>
 */
final class AuditRecordIdGenerator {

    private AuditRecordIdGenerator() {}

    /**
     * Generates an {@link AuditRecordId} for a ContentItem lifecycle event.
     *
     * <p>Format: {@code audit:content-item-{action}:{siteKey}:{contentTypeKey}:{contentItemKey}:{occurredAtMillis}}</p>
     *
     * @param action         the lifecycle action label (e.g. {@code "created"}, {@code "archived"}); must not be null
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type; must not be null
     * @param contentItemKey the content item; must not be null
     * @param occurredAt     the event instant; must not be null
     * @return a deterministic audit record id
     */
    static AuditRecordId contentItemLifecycle(
            final String action,
            final SiteKey siteKey,
            final ContentTypeKey contentTypeKey,
            final ContentItemKey contentItemKey,
            final Instant occurredAt) {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        return AuditRecordId.of(
                "audit:content-item-" + action + ":"
                        + siteKey.value() + ":"
                        + contentTypeKey.value() + ":"
                        + contentItemKey.value() + ":"
                        + occurredAt.toEpochMilli());
    }
}
