package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentTypeKey;

import java.util.Objects;

/**
 * Command to activate a {@link codex.codex.api.model.entity.ContentType},
 * transitioning it from {@code DRAFT} to {@code ACTIVE}.
 */
public record ActivateContentTypeCommand(ContentTypeKey key) {

    public ActivateContentTypeCommand {
        Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Factory method.
     *
     * @param key the key of the content type to activate
     * @return a new {@link ActivateContentTypeCommand}
     */
    public static ActivateContentTypeCommand of(final ContentTypeKey key) {
        return new ActivateContentTypeCommand(key);
    }
}
