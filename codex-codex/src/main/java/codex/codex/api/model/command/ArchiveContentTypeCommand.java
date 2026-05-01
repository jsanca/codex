package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentTypeKey;

import java.util.Objects;

/**
 * Command to archive a {@link codex.codex.api.model.entity.ContentType},
 * transitioning it to {@code ARCHIVED} status.
 */
public record ArchiveContentTypeCommand(ContentTypeKey key) {

    public ArchiveContentTypeCommand {
        Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Factory method.
     *
     * @param key the key of the content type to archive
     * @return a new {@link ArchiveContentTypeCommand}
     */
    public static ArchiveContentTypeCommand of(final ContentTypeKey key) {
        return new ArchiveContentTypeCommand(key);
    }
}
