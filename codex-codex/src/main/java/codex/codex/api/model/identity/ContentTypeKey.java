package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable human-friendly identity for a {@link codex.codex.api.model.entity.ContentType}.
 * <p>
 * Examples: {@code blog-post}, {@code product}, {@code landing-page}.
 * The key is normalized to lowercase and validated on construction.
 */
public record ContentTypeKey(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-_.]*[a-z0-9]$");

    public ContentTypeKey {
        Objects.requireNonNull(value, "content type key cannot be null");
        value = value.trim().toLowerCase();
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "content type key must have at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "content type key cannot have more than " + MAX_LENGTH + " characters");
        }
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "content type key must start and end with a letter or number and may only contain " +
                    "lowercase letters, numbers, hyphens, underscores, and dots");
        }
    }

    /**
     * Creates a {@link ContentTypeKey} from a raw string value.
     *
     * @param value the raw string value; trimmed and lowercased
     * @return a validated {@code ContentTypeKey}
     */
    public static ContentTypeKey of(final String value) {
        return new ContentTypeKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
