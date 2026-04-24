package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable identity used to lookup a site inside Codex.
 * <p>
 * A {@code SiteKey} is not necessarily a domain name. Domains and aliases may
 * change over time, while this key should remain stable for code-level lookups.
 */
public record SiteKey(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-_.]*[a-z0-9]$");

    public SiteKey {
        Objects.requireNonNull(value, "site key cannot be null");

        value = value.trim().toLowerCase();

        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("site key must have at least " + MIN_LENGTH + " characters");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("site key cannot have more than " + MAX_LENGTH + " characters");
        }

        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("site key must start and end with a letter or number and may only contain lowercase letters, numbers, hyphens, underscores, and dots");
        }
    }

    public static SiteKey of(final String value) {
        return new SiteKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
