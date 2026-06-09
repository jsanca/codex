package codex.custos.api.model;

import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Refers to a site as the resource being evaluated.
 * Used for permissions such as {@code site.start} or {@code site.archive}.
 */
public record SiteResourceRef(SiteKey siteKey) implements ResourceRef {

    public SiteResourceRef {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
    }
}
