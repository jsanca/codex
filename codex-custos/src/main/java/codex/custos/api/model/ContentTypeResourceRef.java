package codex.custos.api.model;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Refers to a content type within a site as the resource being evaluated.
 * Used for permissions such as {@code contentType.update} or {@code contentType.delete}.
 */
public record ContentTypeResourceRef(SiteKey siteKey, ContentTypeKey contentTypeKey) implements ResourceRef {

    public ContentTypeResourceRef {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
    }
}
