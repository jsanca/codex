package codex.index.internal;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.index.api.IndexDocumentId;

import java.util.Objects;

/**
 * Factory for deterministic {@link IndexDocumentId} values used across index subscribers
 * and mappers.
 *
 * <p>Centralizes the id format so that upsert and delete operations always target the
 * same document for the same content item.</p>
 */
final class IndexDocumentIds {

    private IndexDocumentIds() {}

    /**
     * Returns the deterministic {@link IndexDocumentId} for a content item.
     *
     * <p>Format: {@code content-item:{siteKey}:{contentTypeKey}:{contentItemKey}}</p>
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type; must not be null
     * @param contentItemKey the content item; must not be null
     * @return a stable document id
     */
    static IndexDocumentId contentItem(
            final SiteKey siteKey,
            final ContentTypeKey contentTypeKey,
            final ContentItemKey contentItemKey) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
        return IndexDocumentId.of(
                "content-item:" + siteKey.value()
                        + ":" + contentTypeKey.value()
                        + ":" + contentItemKey.value());
    }
}
