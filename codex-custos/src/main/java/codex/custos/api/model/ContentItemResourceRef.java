package codex.custos.api.model;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Refers to a specific content item as the resource being evaluated.
 * Used for permissions such as {@code contentItem.publish} or {@code contentItem.update}.
 */
public record ContentItemResourceRef(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey contentItemKey
) implements ResourceRef {

    public ContentItemResourceRef {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
    }
}
