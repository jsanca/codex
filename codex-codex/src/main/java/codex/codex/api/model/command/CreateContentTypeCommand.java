package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentTypeKey;

import java.util.Objects;

/**
 * Command to create a new {@link codex.codex.api.model.entity.ContentType}.
 * The new content type is initially placed in {@code DRAFT} status.
 */
public record CreateContentTypeCommand(ContentTypeKey key, String displayName) {

    public CreateContentTypeCommand {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        displayName = displayName.trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank");
        }
    }

    /**
     * Factory method.
     *
     * @param key         the stable key for the new content type
     * @param displayName the human-readable label
     * @return a new {@link CreateContentTypeCommand}
     */
    public static CreateContentTypeCommand of(final ContentTypeKey key, final String displayName) {
        return new CreateContentTypeCommand(key, displayName);
    }
}
