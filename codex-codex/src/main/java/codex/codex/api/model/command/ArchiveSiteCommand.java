package codex.codex.api.model.command;

import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Represents an immutable request to archive a Site.
 *
 * <p>This is an application command, not a GoF executable command.
 * It carries the intention and required input data for the archive-site use case.
 * The command is interpreted by an application service or command handler.</p>
 */
public record ArchiveSiteCommand(SiteKey key) {

    public ArchiveSiteCommand {
        Objects.requireNonNull(key, "key cannot be null");
    }

    public static ArchiveSiteCommand of(final SiteKey key) {
        return new ArchiveSiteCommand(key);
    }
}
