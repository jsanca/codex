package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command to archive a {@link codex.codex.api.model.entity.ContentType},
 * transitioning it to {@code ARCHIVED} status.
 */
public record ArchiveContentTypeCommand(SiteKey siteKey, ContentTypeKey key) {

    public ArchiveContentTypeCommand {
        Objects.requireNonNull(siteKey, "siteKey cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Factory method using typed keys.
     *
     * @param siteKey the site scope
     * @param key     the key of the content type to archive
     * @return a new {@link ArchiveContentTypeCommand}
     */
    public static ArchiveContentTypeCommand of(final SiteKey siteKey, final ContentTypeKey key) {
        return new ArchiveContentTypeCommand(siteKey, key);
    }

    /**
     * Convenience factory method using raw strings.
     *
     * @param siteKey the site scope as a string
     * @param key     the key of the content type to archive
     * @return a new {@link ArchiveContentTypeCommand}
     */
    public static ArchiveContentTypeCommand of(final String siteKey, final String key) {
        return new ArchiveContentTypeCommand(SiteKey.of(siteKey), ContentTypeKey.of(key));
    }
}
