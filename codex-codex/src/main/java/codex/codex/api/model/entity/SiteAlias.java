package codex.codex.api.model.entity;

import java.util.Objects;

/**
 * External resolution alias for a site.
 * <p>
 * A site alias may represent a hostname, domain, localhost entry,
 * IP address, or another externally meaningful site binding.
 */
public record SiteAlias(String value) {

    public SiteAlias {
        Objects.requireNonNull(value, "SiteAlias value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("SiteAlias value cannot be blank");
        }
    }

    public static SiteAlias of(String value) {
        return new SiteAlias(value);
    }
}
