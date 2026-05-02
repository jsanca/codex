package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable human-friendly identity for a {@link codex.codex.api.model.entity.ContentItem}
 * within a given site and content type scope.
 * <p>
 * Follows the same key vocabulary as {@link SiteKey}, {@link ContentTypeKey}, and {@link FieldKey}.
 * Examples: {@code welcome-to-codex}, {@code home-page}, {@code about-us}, {@code product-123}.
 * <p>
 * This key is not a full path. Future {@code TreeableResource} or {@code CodexResourcePath}
 * support may use this key as one segment in a larger path.
 * <p>
 * The logical identity of a content item is {@code siteKey + contentTypeKey + contentItemKey}.
 */
public record ContentItemKey(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 200;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-_.]*[a-z0-9]$");

    /**
     * Canonical constructor for {@link ContentItemKey}.
     *
     * @param value the raw string value; trimmed, lowercased, and validated
     */
    public ContentItemKey {
        Objects.requireNonNull(value, "content item key cannot be null");
        value = value.trim().toLowerCase();
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "content item key must have at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "content item key cannot have more than " + MAX_LENGTH + " characters");
        }
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "content item key must start and end with a letter or number and may only contain " +
                    "lowercase letters, numbers, hyphens, underscores, and dots");
        }
    }

    /**
     * Creates a {@link ContentItemKey} from a raw string value.
     *
     * @param value the raw string value; trimmed and lowercased
     * @return a validated {@code ContentItemKey}
     */
    public static ContentItemKey of(final String value) {
        return new ContentItemKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
