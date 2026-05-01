package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command to activate a {@link codex.codex.api.model.entity.ContentType},
 * transitioning it from {@code DRAFT} to {@code ACTIVE}.
 */
public record ActivateContentTypeCommand(SiteKey siteKey, ContentTypeKey key) {

    public ActivateContentTypeCommand {
        Objects.requireNonNull(siteKey, "siteKey cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Factory method using typed keys.
     *
     * @param siteKey the site scope
     * @param key     the key of the content type to activate
     * @return a new {@link ActivateContentTypeCommand}
     */
    public static ActivateContentTypeCommand of(final SiteKey siteKey, final ContentTypeKey key) {
        return new ActivateContentTypeCommand(siteKey, key);
    }

    /**
     * Convenience factory method using raw strings.
     *
     * @param siteKey the site scope as a string
     * @param key     the key of the content type to activate
     * @return a new {@link ActivateContentTypeCommand}
     */
    public static ActivateContentTypeCommand of(final String siteKey, final String key) {
        return new ActivateContentTypeCommand(SiteKey.of(siteKey), ContentTypeKey.of(key));
    }
}
