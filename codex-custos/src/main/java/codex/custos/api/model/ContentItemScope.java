package codex.custos.api.model;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Assigns a permission or role scoped to a specific content item.
 */
public record ContentItemScope(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey contentItemKey
) implements ResourceScope {

    public ContentItemScope {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
    }
}
