package codex.custos.api.model;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Assigns a permission or role scoped to a specific content type within a site.
 */
public record ContentTypeScope(SiteKey siteKey, ContentTypeKey contentTypeKey) implements ResourceScope {

    public ContentTypeScope {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
    }
}
