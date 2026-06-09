package codex.custos.api.model;

import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Assigns a permission or role scoped to a specific site.
 * A grant at this scope does not apply to any other site.
 */
public record SiteScope(SiteKey siteKey) implements ResourceScope {

    public SiteScope {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
    }
}
