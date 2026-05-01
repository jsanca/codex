package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command to create a new {@link codex.codex.api.model.entity.ContentType}.
 * The new content type is initially placed in {@code DRAFT} status.
 * <p>
 * The {@code siteKey} determines the scope of the content type. Use {@link SiteKey#SYSTEM}
 * for global (platform-wide) content types shared across all sites. Use a regular
 * {@link SiteKey} for site-specific content types.
 */
public record CreateContentTypeCommand(SiteKey siteKey, ContentTypeKey key, String displayName) {

    public CreateContentTypeCommand {
        Objects.requireNonNull(siteKey, "siteKey cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        displayName = displayName.trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank");
        }
    }

    /**
     * Factory method using typed keys.
     *
     * @param siteKey     the site scope; use {@link SiteKey#SYSTEM} for global content types
     * @param key         the stable key for the new content type within the site scope
     * @param displayName the human-readable label
     * @return a new {@link CreateContentTypeCommand}
     */
    public static CreateContentTypeCommand of(final SiteKey siteKey,
                                              final ContentTypeKey key,
                                              final String displayName) {
        return new CreateContentTypeCommand(siteKey, key, displayName);
    }

    /**
     * Convenience factory method using raw strings.
     *
     * @param siteKey     the site scope as a string; use {@code "system"} for global content types
     * @param key         the stable key for the new content type
     * @param displayName the human-readable label
     * @return a new {@link CreateContentTypeCommand}
     */
    public static CreateContentTypeCommand of(final String siteKey,
                                              final String key,
                                              final String displayName) {
        return new CreateContentTypeCommand(SiteKey.of(siteKey), ContentTypeKey.of(key), displayName);
    }
}
